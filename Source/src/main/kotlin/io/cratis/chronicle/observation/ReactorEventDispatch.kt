// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.json.chronicleGson
import io.opentelemetry.api.common.Attributes

/**
 * Runs one batch of events past a reactor, and reports how far it got.
 *
 * This is everything that happens between the kernel handing over a batch and the client answering
 * for it: choosing the handler, resolving what it asked for, running the middlewares around it,
 * appending whatever it returned, and bracketing a replay with the notifications the reactor asked
 * for. [ReactorsService] is left holding the gRPC conversation.
 *
 * @param registration What was read off the reactor class at registration.
 * @param reactor The reactor instance to dispatch to.
 * @param middlewares Wrapped around every handler invocation.
 * @param arguments Supplies the handler parameters past the event.
 * @param sideEffects Appends whatever a handler returned.
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
     * Observes every event in [events] for [partition].
     *
     * One event failing does not stop the batch: the kernel is told the last position the reactor
     * genuinely got past, so it can resume from there once the cause is fixed rather than from
     * wherever the batch happened to end.
     */
    suspend fun observe(
        partition: String,
        events: List<ObservationReactors.AppendedEvent>
    ): ReactorObservationOutcome {
        val outcome = ReactorObservationOutcome()

        for (appendedEvent in events) {
            val context = appendedEvent.context.toEventContext()

            // The kernel flags the first and last event of a replay rather than sending a separate
            // signal, so the notifications bracket the handling of those two events.
            if (context.observationState.isHeadOfReplay) {
                notify(partition, context, outcome) { replayContext ->
                    registration.replayNotifications.notifyBegan(reactor, replayContext)
                }
            }

            when (val resolution =
                registration.handlers.resolve(context.eventType.id.value, context.observationState)) {
                is ReactorHandlerResolution.Invoke -> invoke(resolution.handler, appendedEvent, context, outcome)

                // A handler deliberately skipped for replay and an event no handler wants are both
                // fully observed - the reactor is caught up past them either way. A skipped
                // @OnceOnly handler produces no side effects either, which is the whole point of
                // taking it out of the replay.
                else -> outcome.observed(context.sequenceNumber)
            }

            if (context.observationState.isTailOfReplay) {
                notify(partition, context, outcome) { replayContext ->
                    registration.replayNotifications.notifyEnded(reactor, replayContext)
                }
            }
        }

        return outcome
    }

    /**
     * Runs a replay notification, recording a failure the same way a failing handler is recorded.
     *
     * A notification that throws has to fail the partition rather than the whole observation: the
     * kernel is then told the position the reactor genuinely got past, and the partition turns up in
     * `failedPartitions` with the message. Letting it escape would tear the stream down instead, and
     * the reactor would silently retry the same batch forever.
     */
    private suspend fun notify(
        partition: String,
        context: EventContext,
        outcome: ReactorObservationOutcome,
        notification: suspend (ReplayContext) -> Unit
    ) {
        try {
            notification(replayContextFor(partition, context.sequenceNumber))
        } catch (e: Exception) {
            outcome.failed(e, "replay notification")
        }
    }

    private suspend fun invoke(
        handler: EventHandlerMethod,
        appendedEvent: ObservationReactors.AppendedEvent,
        context: EventContext,
        outcome: ReactorObservationOutcome
    ) {
        try {
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
            outcome.observed(context.sequenceNumber)
        } catch (e: Exception) {
            outcome.failed(e, handler.function.name)
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

    private fun replayContextFor(partition: String, sequenceNumber: Long) = ReplayContext(
        observerId = registration.id,
        partition = partition,
        sequenceNumber = EventSequenceNumber(sequenceNumber)
    )
}
