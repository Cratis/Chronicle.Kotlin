// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.json.chronicleGson
import io.opentelemetry.api.common.Attributes
import kotlinx.coroutines.CancellationException

/**
 * Runs one delivery of events past a reactor, and reports how far it got in each partition.
 *
 * This is everything that happens between the kernel handing over a page and the client answering
 * for it: calling each batch method once with the events of the types it observes, choosing the
 * per-event handler for every other event, resolving what it asked for, running the middlewares
 * around it, appending whatever it returned, and bracketing a replay with the notifications the
 * reactor asked for. [ReactorsService] is left holding the gRPC conversation.
 *
 * Each event type reaches exactly one method. Each method sees its events in sequence order, but two
 * methods are not ordered relative to each other. A partition settles at the earliest event any
 * method reported a problem for: every event before it counts as observed, and everything from it is
 * delivered again.
 *
 * @param registration What was read off the reactor class at registration.
 * @param reactor The reactor instance to dispatch to.
 * @param middlewares Wrapped around every per-event handler invocation.
 * @param arguments Supplies the per-event handler parameters past the event.
 * @param sideEffects Appends whatever a per-event handler returned.
 * @param traces Produces a span per handler invocation.
 */
internal class ReactorEventDispatch(
    private val registration: ReactorRegistration,
    private val reactor: Any,
    private val middlewares: ReactorMiddlewares,
    private val arguments: ReactorMethodArguments,
    private val sideEffects: ReactorSideEffects,
    private val traces: ChronicleTraces = ChronicleTraces.default
) {
    /**
     * Observes every event of [page], and answers with one outcome per partition, in the page's order.
     *
     * @param context What batch methods are told about the page.
     */
    suspend fun observe(page: DeliveredPage, context: BatchContext): List<PartitionOutcome> {
        val problems = mutableListOf<PartitionProblem>()

        // The kernel flags the first and last event of a replay rather than sending a separate signal,
        // so the notifications bracket the handling of the whole page.
        notifyReplay(page, problems, { it.isHeadOfReplay }) { replayContext ->
            registration.replayNotifications.notifyBegan(reactor, replayContext)
        }

        for (method in registration.handlers.batches) {
            problems += observeInBatch(method, page, context)
        }

        for (partition in page.partitions) {
            observeOneByOne(partition)?.let { problems += it }
        }

        notifyReplay(page, problems, { it.isTailOfReplay }) { replayContext ->
            registration.replayNotifications.notifyEnded(reactor, replayContext)
        }

        return page.partitions.map { PartitionSettlement.settle(it.partition, it.sequenceNumbers, problems) }
    }

    /**
     * Calls [method] once with every event of [page] it observes, across partitions and in sequence
     * order, and collects the problems it reported. Throwing fails every partition in the batch at its
     * first event in it.
     */
    private suspend fun observeInBatch(method: BatchMethod, page: DeliveredPage, context: BatchContext): List<PartitionProblem> {
        val problems = mutableListOf<PartitionProblem>()
        val observed = mutableListOf<Pair<String, EventAndContext>>()

        for (partition in page.partitions) {
            for (appendedEvent in partition.events) {
                val eventContext = appendedEvent.context
                val eventClass = method.eventTypes[eventContext.eventType.id.value] ?: continue

                // A batch method marked @OnceOnly is left out of a replay, exactly like a per-event one:
                // the replayed events count as observed without it.
                if (method.isOnceOnly && eventContext.observationState.isReplay) continue

                try {
                    observed += partition.partition to EventAndContext(chronicleGson.fromJson(appendedEvent.content, eventClass.java), eventContext)
                } catch (e: Exception) {
                    problems += PartitionProblem.failed(partition.partition, eventContext.sequenceNumber, e, method.name)
                }
            }
        }

        if (observed.isEmpty()) return problems
        observed.sortBy { it.second.context.sequenceNumber }

        val batch = EventBatch(observed.map { it.second }).apply { canDefer = context.canDefer }
        try {
            traces.span("Chronicle observe batch ${method.name}", batchAttributes(batch)) {
                if (method.takesContextAt(2)) method.invoke(reactor, batch, context) else method.invoke(reactor, batch)
            }
            problems += batch.outcomes.map { outcome ->
                // The batch speaks in event sources; the kernel in partitions, which name them.
                val partition = observed.first { it.second.context.eventSourceId == outcome.partition }.first
                outcome.copy(partition = partition).toProblem()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problems += observed.groupBy { it.first }.map { (partition, events) ->
                PartitionProblem.failed(partition, events.first().second.context.sequenceNumber, e, method.name)
            }
        }

        return problems
    }

    /**
     * Observes the events of [partition] that no batch method observes, one at a time and in order,
     * stopping at the first that fails - the partition is delivered again from there, so the events
     * after it are left for then.
     *
     * @return The problem the partition stopped at, or `null` when every event was observed.
     */
    private suspend fun observeOneByOne(partition: DeliveredPartition): PartitionProblem? {
        for (appendedEvent in partition.events) {
            val context = appendedEvent.context
            if (registration.handlers.batchFor(context.eventType.id.value) != null) continue

            when (val resolution = registration.handlers.resolve(context.eventType.id.value, context.observationState)) {
                is ReactorHandlerResolution.Invoke ->
                    invoke(resolution.handler, appendedEvent)?.let { error ->
                        return PartitionProblem.failed(partition.partition, context.sequenceNumber, error, resolution.handler.function.name)
                    }

                // A handler deliberately skipped for replay and an event no handler wants are both
                // fully observed - the reactor is caught up past them either way. A skipped
                // @OnceOnly handler produces no side effects either, which is the whole point of
                // taking it out of the replay.
                else -> Unit
            }
        }
        return null
    }

    /**
     * Runs a replay notification for every event of [page] matching [flagged], recording a failure the
     * same way a failing handler is recorded.
     *
     * A notification that throws has to fail the partition rather than the whole observation: the
     * kernel is then told the position the reactor genuinely got past, and the partition turns up in
     * `failedPartitions` with the message. Letting it escape would tear the stream down instead, and
     * the reactor would silently retry the same batch forever.
     */
    private suspend fun notifyReplay(
        page: DeliveredPage,
        problems: MutableList<PartitionProblem>,
        flagged: (EventObservationState) -> Boolean,
        notification: suspend (ReplayContext) -> Unit
    ) {
        for (partition in page.partitions) {
            for (event in partition.events.filter { flagged(it.context.observationState) }) {
                try {
                    notification(replayContextFor(partition.partition, event.context.sequenceNumber))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    problems += PartitionProblem.failed(partition.partition, event.context.sequenceNumber, e, "replay notification")
                }
            }
        }
    }

    /** Invokes [handler] for [appendedEvent], returning what it failed with, or `null` when it succeeded. */
    private suspend fun invoke(handler: EventHandlerMethod, appendedEvent: AppendedEvent): Exception? {
        val context = appendedEvent.context
        return try {
            // The span covers the middlewares as well as the handler, so a middleware that is itself
            // slow shows up as part of what handling the event cost - which is the number anyone
            // reading the trace came for.
            val result = traces.span("Chronicle observe ${context.eventType.id.value}", attributesFor(context)) {
                val event = chronicleGson.fromJson(appendedEvent.content, handler.eventClass.java)
                middlewares.invoke(context, event) {
                    handler.invoke(reactor, event, *arguments.resolve(handler, context).toTypedArray())
                }
            }
            sideEffects.append(result, context.eventSourceId)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }
    }

    /** What a reader of a trace needs to place this invocation: which observer, which event, where. */
    private fun attributesFor(context: EventContext): Attributes = Attributes.builder()
        .put(ChronicleTraces.OBSERVER_ID, registration.id)
        .put(ChronicleTraces.EVENT_TYPE, context.eventType.id.value)
        .put(ChronicleTraces.EVENT_SOURCE_ID, context.eventSourceId)
        .put(ChronicleTraces.EVENT_SEQUENCE_ID, registration.eventSequenceId)
        .put(ChronicleTraces.SEQUENCE_NUMBER, context.sequenceNumber)
        .put(ChronicleTraces.IS_REPLAY, context.observationState.isReplay)
        .build()

    /** What a reader of a trace needs to place a batch: which observer, where, and how many events. */
    private fun batchAttributes(batch: EventBatch): Attributes = Attributes.builder()
        .put(ChronicleTraces.OBSERVER_ID, registration.id)
        .put(ChronicleTraces.EVENT_SEQUENCE_ID, registration.eventSequenceId)
        .put(ChronicleTraces.EVENT_COUNT, batch.size.toLong())
        .build()

    private fun replayContextFor(partition: String, sequenceNumber: Long) = ReplayContext(
        observerId = registration.id,
        partition = partition,
        sequenceNumber = EventSequenceNumber(sequenceNumber)
    )
}
