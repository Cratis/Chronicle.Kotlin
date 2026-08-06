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
 */
internal class ReactorHandlers(
    private val live: Map<String, EventHandlerMethod>,
    private val replay: Map<String, EventHandlerMethod>,
    private val onceOnly: Set<KFunction<*>>
) {
    /**
     * Every event type handled, whichever path handles it. An event type handled only by a replay
     * handler still has to be subscribed to, or the replay would have nothing to deliver.
     */
    val eventTypes: Map<String, KClass<*>> =
        (replay + live).mapValues { (_, handler) -> handler.eventClass }

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
         * is a class annotated with `@EventType` is a handler for that event type.
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
                requireDispatchable(handler, reactorClass, arguments)

                if (function.findAnnotation<OnceOnly>() != null) {
                    onceOnly.add(function)
                }

                val target = if (function.findAnnotation<Replay>() != null) replay else live
                target[handler.eventTypeId] = handler
            }

            if (live.isEmpty() && replay.isEmpty()) {
                throw ObserverHasNoHandlers(
                    reactorClass,
                    "A reactor handler is a public method whose first parameter is a class annotated with @EventType."
                )
            }

            return ReactorHandlers(live, replay, onceOnly)
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
