// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.json.chronicleGson
import io.opentelemetry.api.common.Attributes
import kotlinx.coroutines.CancellationException

/**
 * Folds the events of one event source through a reducer: either one call to its batch method, or one
 * call per event to its per-event handlers, in order.
 *
 * A reducer is a fold, so a failure anywhere fails the whole event source - no state is written for it,
 * and its events are folded again from the stored state once the failure is recovered.
 *
 * @param registration What was read off the reducer class at registration.
 * @param reducer The reducer instance.
 * @param traces Produces a span per fold step.
 */
internal class ReducerFold(
    private val registration: ReducerRegistration,
    private val reducer: Any,
    private val traces: ChronicleTraces = ChronicleTraces.default
) {
    /** Folds [events], all of one event source and in order, starting from [initial]. */
    suspend fun reduce(events: List<AppendedEvent>, initial: Any?, context: BatchContext): ReduceResult {
        val batch = registration.batch
        return if (batch != null) reduceInBatch(batch, events, initial, context) else reduceOneByOne(events, initial)
    }

    private suspend fun reduceOneByOne(events: List<AppendedEvent>, initial: Any?): ReduceResult {
        var state = initial
        var lastObserved = PartitionOutcome.NONE_OBSERVED

        for (appendedEvent in events) {
            val handler = registration.handlers[appendedEvent.context.eventType.id.value]
            if (handler != null) {
                try {
                    state = traces.span("Chronicle reduce ${appendedEvent.context.eventType.id.value}", attributesFor(appendedEvent.context)) {
                        val event = chronicleGson.fromJson(appendedEvent.content, handler.eventClass.java)
                        handler.invokeReducer(reducer, event, state, appendedEvent.context)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return ReduceResult.Failed(e, handler.function.name)
                }
            }
            lastObserved = appendedEvent.context.sequenceNumber
        }

        return ReduceResult.Reduced(state, lastObserved)
    }

    private suspend fun reduceInBatch(method: BatchMethod, events: List<AppendedEvent>, initial: Any?, context: BatchContext): ReduceResult {
        val lastObserved = events.lastOrNull()?.context?.sequenceNumber ?: PartitionOutcome.NONE_OBSERVED
        val observed = try {
            events.mapNotNull { appendedEvent ->
                method.eventTypes[appendedEvent.context.eventType.id.value]?.let { eventClass ->
                    EventAndContext(chronicleGson.fromJson(appendedEvent.content, eventClass.java), appendedEvent.context)
                }
            }
        } catch (e: Exception) {
            return ReduceResult.Failed(e, method.name)
        }
        if (observed.isEmpty()) return ReduceResult.Reduced(initial, lastObserved)

        val batch = EventBatch(observed).apply { canDefer = context.canDefer }
        val state = try {
            traces.span("Chronicle reduce batch ${method.name}", batchAttributes(batch)) {
                if (method.takesContextAt(3)) method.invoke(reducer, batch, initial, context) else method.invoke(reducer, batch, initial)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ReduceResult.Failed(e, method.name)
        }

        // The earliest problem reported wins; a reducer fails or defers the event source as a whole, as
        // a fold cannot keep the part before it.
        val earliest = batch.outcomes.minByOrNull { it.at } ?: return ReduceResult.Reduced(state, lastObserved)
        return when (earliest.kind) {
            BatchOutcome.Kind.Deferred -> ReduceResult.Deferred(earliest.retryAfter)
            BatchOutcome.Kind.Failed -> ReduceResult.Failed(earliest.error ?: PartitionFailedInBatch(earliest.partition), method.name)
        }
    }

    /** What a reader of a trace needs to place this fold: which reducer, which event, where. */
    private fun attributesFor(context: EventContext): Attributes = Attributes.builder()
        .put(ChronicleTraces.OBSERVER_ID, registration.id)
        .put(ChronicleTraces.EVENT_TYPE, context.eventType.id.value)
        .put(ChronicleTraces.EVENT_SOURCE_ID, context.eventSourceId)
        .put(ChronicleTraces.EVENT_SEQUENCE_ID, registration.eventSequenceId)
        .put(ChronicleTraces.SEQUENCE_NUMBER, context.sequenceNumber)
        .put(ChronicleTraces.IS_REPLAY, context.observationState.isReplay)
        .build()

    private fun batchAttributes(batch: EventBatch): Attributes = Attributes.builder()
        .put(ChronicleTraces.OBSERVER_ID, registration.id)
        .put(ChronicleTraces.EVENT_SEQUENCE_ID, registration.eventSequenceId)
        .put(ChronicleTraces.EVENT_COUNT, batch.size.toLong())
        .apply { batch.firstOrNull()?.let { put(ChronicleTraces.EVENT_SOURCE_ID, it.context.eventSourceId) } }
        .build()
}
