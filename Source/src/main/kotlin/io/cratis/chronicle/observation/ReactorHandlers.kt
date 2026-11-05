// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventObservationState
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * The handler methods of a reactor, split by whether they take over during a replay.
 *
 * A reactor can hold two handlers for the same event type: the everyday one, and one marked [Replay]
 * that takes over while the observer is being replayed. Either can additionally be marked [OnceOnly],
 * which excludes it from replay entirely.
 *
 * @property live The handlers to run as events happen, keyed by event type identifier.
 * @property replay The handlers marked [Replay], keyed by event type identifier.
 * @property onceOnly The handler methods marked [OnceOnly], which never run for a replayed event.
 * @property batches The batch methods, each taking an [EventBatch] of the event types it [Observes].
 *   An event type a batch method observes reaches no per-event handler.
 */
internal class ReactorHandlers(
    private val live: Map<String, EventHandlerMethod>,
    private val replay: Map<String, EventHandlerMethod>,
    private val onceOnly: Set<KFunction<*>>,
    val batches: List<BatchMethod> = emptyList()
) {
    /**
     * Every event type handled, whichever path handles it. An event type handled only by a replay
     * handler still has to be subscribed to, or the replay would have nothing to deliver.
     */
    val eventTypes: Map<String, KClass<*>> =
        (replay + live).mapValues { (_, handler) -> handler.eventClass } +
            batches.flatMap { it.eventTypes.entries }.associate { it.key to it.value }

    /** The batch method observing the event type identified by [eventTypeId], if one does. */
    fun batchFor(eventTypeId: String): BatchMethod? = batches.firstOrNull { it.observes(eventTypeId) }

    /**
     * Works out which handler to run for an event type, given the state the event arrives in.
     *
     * [OnceOnly] on a handler method means that handler is excluded from replay. The class-level
     * placement keeps the whole reactor from being replayed and is applied at registration; the
     * method-level placement can only be honored here, where the event arrives carrying the state it
     * is observed in.
     */
    fun resolve(eventTypeId: String, observationState: EventObservationState): ReactorHandlerResolution {
        // A replay handler takes over for the duration of the replay, and the live handler does not
        // also run. Without one, the live handler keeps running during replay - which is what every
        // reactor written before this existed relies on.
        if (observationState.isReplay) {
            replay[eventTypeId]?.let { handler ->
                return if (handler.function in onceOnly) {
                    ReactorHandlerResolution.SkippedForReplay
                } else {
                    ReactorHandlerResolution.Invoke(handler)
                }
            }
        }

        val handler = live[eventTypeId] ?: return ReactorHandlerResolution.NotHandled

        return if (observationState.isReplay && handler.function in onceOnly) {
            ReactorHandlerResolution.SkippedForReplay
        } else {
            ReactorHandlerResolution.Invoke(handler)
        }
    }

    companion object {
        /**
         * Builds the handler set for [reactorClass] by convention: any method whose first parameter
         * is a class annotated with `@EventType` is a handler for that event type, and any method whose
         * first parameter is an [EventBatch] is a batch method for the event types it [Observes].
         */
        fun from(
            reactorClass: KClass<*>,
            arguments: ReactorMethodArguments = ReactorMethodArguments.contextOnly
        ): ReactorHandlers {
            val live = mutableMapOf<String, EventHandlerMethod>()
            val replay = mutableMapOf<String, EventHandlerMethod>()
            val onceOnly = mutableSetOf<KFunction<*>>()

            for (function in reactorClass.memberFunctions) {
                val handler = EventHandlerMethod.from(function) ?: continue
                BatchMethods.requireNoBatchContext(reactorClass, listOf(handler))
                requireDispatchable(handler, reactorClass, arguments)

                if (function.findAnnotation<OnceOnly>() != null) {
                    onceOnly.add(function)
                }

                val target = if (function.findAnnotation<Replay>() != null) replay else live
                target[handler.eventTypeId] = handler
            }

            val batches = BatchMethods.from(reactorClass)
            batches.forEach { requireDispatchable(it, reactorClass) }

            // Each event type reaches exactly one method, so a reader never has to work out which of two
            // methods an event goes to.
            batches.flatMap { it.eventTypes.keys }
                .firstOrNull { it in live || it in replay }
                ?.let { throw EventTypeObservedByMoreThanOneMethod(reactorClass, it) }

            if (live.isEmpty() && replay.isEmpty() && batches.isEmpty()) {
                throw ObserverHasNoHandlers(
                    reactorClass,
                    "A reactor handler is a public method whose first parameter is a class annotated with @EventType, " +
                        "or a batch method taking an EventBatch and declaring the event types it @Observes."
                )
            }

            return ReactorHandlers(live, replay, onceOnly, batches)
        }

        /**
         * A reactor batch method takes the [EventBatch], and optionally the [BatchContext] after it,
         * and returns nothing. Returning events from a batch is not supported: a batch method that has
         * to append does so through the event log it is given, since a returned event would not belong
         * to any one of the events in the batch.
         */
        private fun requireDispatchable(method: BatchMethod, reactorClass: KClass<*>) {
            // Index 0 is the instance receiver, so the two valid shapes arrive as 2 and 3.
            val parameters = method.function.parameters.size
            if (parameters > 3 || (parameters == 3 && !method.takesContextAt(2))) {
                throw InvalidBatchMethod(
                    reactorClass,
                    method.name,
                    "a reactor batch method takes the EventBatch, and optionally a BatchContext after it"
                )
            }

            if (method.function.returnType.classifier != Unit::class) {
                throw InvalidBatchMethod(
                    reactorClass,
                    method.name,
                    "a reactor batch method returns nothing. Returning events from a batch is not supported - " +
                        "append through the event log instead"
                )
            }

            if (method.function.findAnnotation<Replay>() != null) {
                throw InvalidBatchMethod(
                    reactorClass,
                    method.name,
                    "@Replay is for per-event handlers. A batch method is told it is replaying by BatchContext.origin"
                )
            }
        }

        /**
         * A reactor handler takes the event first, suspending or not. Everything after it has to be
         * something the client can supply for each event - the [io.cratis.chronicle.events.EventContext],
         * or whatever the configured resolvers claim. A parameter nothing can supply is rejected here
         * rather than failing on every event the reactor is given.
         */
        private fun requireDispatchable(
            handler: EventHandlerMethod,
            reactorClass: KClass<*>,
            arguments: ReactorMethodArguments
        ) = arguments.requireResolvable(handler, reactorClass)
    }
}
