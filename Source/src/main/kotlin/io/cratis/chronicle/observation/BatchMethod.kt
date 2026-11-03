// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

/**
 * A method of a reactor or reducer that takes an [EventBatch], together with the event types it
 * observes through [Observes].
 *
 * @property function The method to invoke.
 * @property eventTypes The event types observed, keyed by event type identifier.
 * @property isOnceOnly Whether the method is marked [OnceOnly], so replayed events never reach it.
 */
internal class BatchMethod(
    val function: KFunction<*>,
    val eventTypes: Map<String, KClass<*>>,
    val isOnceOnly: Boolean,
) {
    /** The method's name, for messages and traces. */
    val name: String get() = function.name

    /** Whether the method observes the event type identified by [eventTypeId]. */
    fun observes(eventTypeId: String): Boolean = eventTypeId in eventTypes

    /** Whether the parameter at [index] - counting the instance receiver at 0 - is a [BatchContext]. */
    fun takesContextAt(index: Int): Boolean =
        function.parameters.getOrNull(index)?.type?.classifier == BatchContext::class

    /**
     * Invokes the method with the receiver [observer] and [arguments], awaiting it when it suspends.
     *
     * `callSuspend` serves a plain method and a `suspend` one alike, and Java methods too.
     */
    suspend fun invoke(observer: Any, vararg arguments: Any?): Any? = function.callSuspend(observer, *arguments)

    companion object {
        /** The identifier of an event type class, or `null` when the class is not an event type. */
        fun eventTypeIdOf(eventClass: KClass<*>): String? {
            val eventType = eventClass.findAnnotation<EventType>() ?: return null
            return eventType.id.ifEmpty { eventClass.simpleName }
        }
    }
}
