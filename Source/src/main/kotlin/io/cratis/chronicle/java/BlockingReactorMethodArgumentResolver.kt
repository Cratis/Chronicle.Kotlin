// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.IReactorMethodArgumentResolver
import kotlin.reflect.KParameter

/**
 * A reactor method argument resolver written in Java.
 *
 * [IReactorMethodArgumentResolver.resolve] is suspending, which Java cannot implement - a `suspend`
 * method carries a hidden continuation parameter on the JVM. Implement this instead and the client
 * adapts it: a resolver discovered as one of these is consulted exactly like a Kotlin one.
 *
 * ```java
 * public class ClockArgument implements BlockingReactorMethodArgumentResolver {
 *     public boolean canResolve(KParameter parameter) {
 *         return parameter.getType().getClassifier() == Reflection.getOrCreateKotlinClass(Clock.class);
 *     }
 *
 *     public Object resolve(KParameter parameter, EventContext context) {
 *         return Clock.systemUTC();
 *     }
 * }
 * ```
 *
 * [resolve] blocks the coroutine it runs on, so keep it to work that is genuinely quick. Anything
 * that waits on I/O belongs in a Kotlin resolver that can suspend.
 */
interface BlockingReactorMethodArgumentResolver {
    /**
     * Whether this resolver knows how to supply [parameter].
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
    fun resolve(parameter: KParameter, context: EventContext): Any?
}

/** Presents this Java resolver as an [IReactorMethodArgumentResolver] so it can be consulted alongside the rest. */
internal fun BlockingReactorMethodArgumentResolver.asArgumentResolver(): IReactorMethodArgumentResolver =
    object : IReactorMethodArgumentResolver {
        override fun canResolve(parameter: KParameter): Boolean =
            this@asArgumentResolver.canResolve(parameter)

        override suspend fun resolve(parameter: KParameter, context: EventContext): Any? =
            this@asArgumentResolver.resolve(parameter, context)
    }
