// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import kotlin.reflect.KParameter

/**
 * Supplies a reactor handler parameter that is neither the event nor its [EventContext].
 *
 * A reactor handler takes the event, and optionally the context. Anything past that is resolved per
 * invocation through these, which is what lets a handler ask for the current state of a read model,
 * or for a collaborator it would rather not hold for its whole lifetime.
 *
 * The client ships one of these: the current instance of a read model, keyed by the event source the
 * event belongs to. Implement this to add your own - a container-backed resolver, say, so a handler
 * can take a service the way a Spring `@Service` method does. Every implementation on the classpath
 * is discovered and consulted in turn, and the first that says it can resolve a parameter is asked to.
 *
 * A parameter no resolver claims is rejected at registration rather than failing on every event.
 *
 * ```kotlin
 * class ClockArgument : IReactorMethodArgumentResolver {
 *     override fun canResolve(parameter: KParameter) =
 *         parameter.type.classifier == Clock::class
 *
 *     override suspend fun resolve(parameter: KParameter, context: EventContext): Any =
 *         Clock.systemUTC()
 * }
 * ```
 */
interface IReactorMethodArgumentResolver {
    /**
     * Whether this resolver knows how to supply [parameter].
     *
     * Answer on the parameter's declared type, not on anything about the event - this is asked once
     * per handler at registration as well as per invocation.
     *
     * @param parameter The parameter to supply.
     * @return `true` when [resolve] can supply it.
     */
    fun canResolve(parameter: KParameter): Boolean

    /**
     * Supplies the value for [parameter] for the event described by [context].
     *
     * @param parameter The parameter to supply.
     * @param context The context of the event being handled.
     * @return The value to pass, which may be `null` when the parameter accepts it.
     */
    suspend fun resolve(parameter: KParameter, context: EventContext): Any?
}
