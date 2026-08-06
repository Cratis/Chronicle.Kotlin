// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Everything read off a reactor class that the kernel needs in order to register it.
 *
 * @property id The reactor identifier, defaulting to the class simple name.
 * @property eventSequenceId The event sequence to observe, defaulting to the event log.
 * @property isReplayable Whether the kernel may replay this reactor at all.
 * @property tags Descriptive labels for the reactor itself.
 * @property filters Narrows which events the kernel delivers.
 * @property handlers The handler methods, and how they are chosen per event.
 * @property replayNotifications The replay begin and end methods, when the reactor asked to be told.
 */
internal data class ReactorRegistration(
    val id: String,
    val eventSequenceId: String,
    val isReplayable: Boolean,
    val tags: List<String>,
    val filters: ObserverFilters,
    val handlers: ReactorHandlers,
    val replayNotifications: ReplayNotifications
) {
    companion object {
        /**
         * Reads [reactorClass]'s annotations and handler methods.
         *
         * [arguments] decides which handler parameters past the event are supportable, so a handler
         * asking for something nothing can supply is rejected here.
         */
        fun from(
            reactorClass: KClass<*>,
            arguments: ReactorMethodArguments = ReactorMethodArguments.contextOnly
        ): ReactorRegistration {
            val annotation = reactorClass.findAnnotation<Reactor>()

            return ReactorRegistration(
                id = annotation?.id?.ifEmpty { null } ?: reactorClass.simpleName!!,

                eventSequenceId = EventSequence.idOf(reactorClass, annotation?.eventSequence),

                // A class-level @OnceOnly registers the reactor as non-replayable, so a replay never
                // starts for it. The method-level placement is honored per event, when dispatching.
                isReplayable = reactorClass.findAnnotation<OnceOnly>() == null,

                tags = ObserverFilters.tagsOf(reactorClass),
                filters = ObserverFilters.from(reactorClass),

                handlers = ReactorHandlers.from(reactorClass, arguments),
                replayNotifications = ReplayNotifications.from(reactorClass)
            )
        }
    }
}
