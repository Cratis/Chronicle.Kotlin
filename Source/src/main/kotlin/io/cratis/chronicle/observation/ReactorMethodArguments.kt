// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * Works out what to pass a reactor handler beyond the event itself.
 *
 * A handler's first parameter is always the event. Everything after it is either the [EventContext],
 * which the client supplies directly, or something a [IReactorMethodArgumentResolver] knows how to
 * produce for the event being handled.
 *
 * @property resolvers The resolvers to consult, in the order they were discovered.
 */
class ReactorMethodArguments(private val resolvers: List<IReactorMethodArgumentResolver>) {

    /**
     * Checks that every parameter of [handler] past the event can actually be supplied, and throws
     * [InvalidHandlerSignature] naming the first one that cannot.
     *
     * Doing this at registration is the whole point: a parameter nothing can supply would otherwise
     * fail on every single event the reactor is given, long after the signature left anyone's mind.
     */
    internal fun requireResolvable(handler: EventHandlerMethod, reactorClass: KClass<*>) {
        for (parameter in handler.argumentParameters) {
            if (parameter.type.classifier == EventContext::class) continue
            if (resolvers.any { it.canResolve(parameter) }) continue

            handler.reject(
                reactorClass,
                "nothing can supply its '${parameter.name}' parameter. A reactor handler takes the " +
                    "event, and after that an EventContext, a read model, or anything an " +
                    "IReactorMethodArgumentResolver claims"
            )
        }
    }

    /**
     * The arguments to invoke [handler] with, in declaration order, for the event described by
     * [context].
     */
    internal suspend fun resolve(handler: EventHandlerMethod, context: EventContext): List<Any?> =
        handler.argumentParameters.map { parameter ->
            if (parameter.type.classifier == EventContext::class) {
                context
            } else {
                resolvers.first { it.canResolve(parameter) }.resolve(parameter, context)
            }
        }

    companion object {
        /**
         * Only the event and its context, which is every reactor written before resolvers existed.
         */
        val contextOnly: ReactorMethodArguments = ReactorMethodArguments(emptyList())
    }
}
