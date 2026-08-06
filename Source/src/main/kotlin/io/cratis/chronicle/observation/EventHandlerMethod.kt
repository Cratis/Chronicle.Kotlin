// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

/**
 * A single handler method on a reactor or reducer, together with the event type it handles.
 *
 * @property function The method to invoke.
 * @property eventClass The class of the event the method takes as its first parameter.
 * @property eventTypeId The identifier of the event type, as registered with the kernel.
 */
internal data class EventHandlerMethod(
    val function: KFunction<*>,
    val eventClass: KClass<*>,
    val eventTypeId: String
) {
    /** How many parameters the method takes, counting the instance receiver at index 0. */
    val parameterCount: Int get() = function.parameters.size

    /** Whether the parameter at [index] is an [EventContext]. */
    fun takesContextAt(index: Int): Boolean =
        function.parameters.getOrNull(index)?.type?.classifier == EventContext::class

    /**
     * Invokes the handler with [arguments], awaiting it when it suspends.
     *
     * `callSuspend` is used for every handler, suspending or not: a plain function goes straight
     * through it, and a `suspend` one is awaited rather than being handed a continuation it would
     * reject. Both observers already dispatch from inside a coroutine, so a handler that suspends
     * frees the thread instead of blocking it.
     */
    suspend fun invoke(observer: Any, vararg arguments: Any?): Any? =
        function.callSuspend(observer, *arguments)

    /** Throws [InvalidHandlerSignature] with [reason] for this handler. */
    fun reject(observerClass: KClass<*>, reason: String): Nothing =
        throw InvalidHandlerSignature(observerClass, function.name, reason)

    companion object {
        /**
         * Reads [function] as a handler method, or returns `null` when it is not shaped like one -
         * it takes no event, or its first parameter is not a class annotated with [EventType].
         */
        fun from(function: KFunction<*>): EventHandlerMethod? {
            // Index 0 is the instance receiver, so a handler has at least two parameters.
            val eventParameter = function.parameters.getOrNull(1) ?: return null
            val eventClass = eventParameter.type.classifier as? KClass<*> ?: return null
            val eventType = eventClass.findAnnotation<EventType>() ?: return null
            val eventTypeId = eventType.id.ifEmpty { eventClass.simpleName ?: return null }
            return EventHandlerMethod(function, eventClass, eventTypeId)
        }
    }
}
