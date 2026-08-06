// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import io.cratis.chronicle.json.chronicleGson

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
 */
internal class ReactorEventDispatch(
    private val registration: ReactorRegistration,
    private val reactor: Any,
    private val middlewares: ReactorMiddlewares,
    private val arguments: ReactorMethodArguments,
    private val sideEffects: ReactorSideEffects
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
            // signal, so the notifications bracket the handling of those two events. A notification
            // that throws fails the partition just as a handler would: a reactor told a replay began
            // and never told it ended is worse off than one that stops and says so.
            if (context.observationState.isHeadOfReplay) {
                registration.replayNotifications.notifyBegan(reactor, replayContextFor(partition, context.sequenceNumber))
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
                registration.replayNotifications.notifyEnded(reactor, replayContextFor(partition, context.sequenceNumber))
            }
        }

        return outcome
    }

    private suspend fun invoke(
        handler: EventHandlerMethod,
        appendedEvent: ObservationReactors.AppendedEvent,
        context: io.cratis.chronicle.events.EventContext,
        outcome: ReactorObservationOutcome
    ) {
        try {
            val event = chronicleGson.fromJson(appendedEvent.content, handler.eventClass.java)
            val result = middlewares.invoke(context, event) {
                handler.invoke(reactor, event, *arguments.resolve(handler, context).toTypedArray())
            }
            sideEffects.append(result, context.eventSourceId)
            outcome.observed(context.sequenceNumber)
        } catch (e: Exception) {
            outcome.failed(e, handler.function.name)
        }
    }

    private fun replayContextFor(partition: String, sequenceNumber: Long) = ReplayContext(
        observerId = registration.id,
        partition = partition,
        sequenceNumber = io.cratis.chronicle.eventSequences.EventSequenceNumber(sequenceNumber)
    )
}
