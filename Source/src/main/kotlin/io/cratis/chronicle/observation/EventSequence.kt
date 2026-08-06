// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Points an observer - a reactor, a reducer or a projection - at the event sequence it observes.
 *
 * This is the standalone alternative to the `eventSequence` parameter on `@Reactor`, `@Reducer` and
 * `@Projection`, and mirrors C#'s `[EventSequence]`. Reach for it when the sequence is the only thing
 * being configured, so the observer keeps its conventional identifier:
 *
 * ```
 * @EventSequence("outbox")
 * class ShippingNotifications { ... }
 * ```
 *
 * When both this and the parameter are present, this wins - matching the .NET client, where the
 * standalone attribute takes priority over the one on the observer attribute.
 *
 * The parameter is named `value` so that Java callers can use the shorthand form,
 * `@EventSequence("outbox")`, rather than having to spell out an element name.
 *
 * @property value The identifier of the event sequence to observe.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventSequence(val value: String) {
    companion object {
        /**
         * Resolves the event sequence [type] observes, given the [declared] value from its
         * `@Reactor`, `@Reducer` or `@Projection` annotation.
         *
         * An unspecified event sequence means the event log, which is where observers observe from
         * unless they deliberately target another sequence.
         */
        internal fun idOf(type: KClass<*>, declared: String?): String =
            type.findAnnotation<EventSequence>()?.value?.ifEmpty { null }
                ?: declared?.ifEmpty { null }
                ?: EventSequenceId.eventLog.value
    }
}
