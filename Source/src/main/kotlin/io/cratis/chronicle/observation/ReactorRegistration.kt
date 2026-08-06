// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
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
 */
internal data class ReactorRegistration(
    val id: String,
    val eventSequenceId: String,
    val isReplayable: Boolean,
    val tags: List<String>,
    val filters: ObserverFilters,
    val handlers: ReactorHandlers
) {
    companion object {
        /** Reads [reactorClass]'s annotations and handler methods. */
        fun from(reactorClass: KClass<*>): ReactorRegistration {
            val annotation = reactorClass.findAnnotation<Reactor>()

            return ReactorRegistration(
                id = annotation?.id?.ifEmpty { null } ?: reactorClass.simpleName!!,

                // An unspecified event sequence means the event log, which is where reactors observe
                // from unless they deliberately target another sequence.
                eventSequenceId = annotation?.eventSequence?.ifEmpty { null }
                    ?: EventSequenceId.eventLog.value,

                // A class-level @OnceOnly registers the reactor as non-replayable, so a replay never
                // starts for it. The method-level placement is honored per event, when dispatching.
                isReplayable = reactorClass.findAnnotation<OnceOnly>() == null,

                tags = ObserverFilters.tagsOf(reactorClass),
                filters = ObserverFilters.from(reactorClass),

                handlers = ReactorHandlers.from(reactorClass)
            )
        }
    }
}
