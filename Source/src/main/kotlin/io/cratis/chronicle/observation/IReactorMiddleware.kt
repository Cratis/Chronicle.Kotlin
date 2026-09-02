// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext

/**
 * Wraps every reactor handler invocation, so cross-cutting concerns stay out of reactor code.
 *
 * Tracing, logging, and metrics all want to happen around every handler and
 * belong to none of them. Put them here and a reactor stays a description of what happens when a
 * fact arrives.
 *
 * Every middleware on the classpath is discovered and applied, in the order the classes are found.
 * [beforeInvoke] runs outermost-first and [afterInvoke] runs in reverse, so a middleware that opens
 * something in the first is the last to close it in the second - the nesting you would write by hand.
 *
 * [afterInvoke] runs whether the handler succeeded or threw, so anything opened is always closed.
 * A middleware that throws fails the event like any other error would.
 *
 * ```kotlin
 * class HandlerTiming : IReactorMiddleware {
 *     private val started = ConcurrentHashMap<Pair<UUID, Long>, Long>()
 *
 *     override suspend fun beforeInvoke(context: EventContext, event: Any) {
 *         started[context.correlationId to context.sequenceNumber] = System.nanoTime()
 *     }
 *
 *     override suspend fun afterInvoke(context: EventContext, event: Any) {
 *         val began = started.remove(context.correlationId to context.sequenceNumber) ?: return
 *         println("${event::class.simpleName} took ${System.nanoTime() - began}ns")
 *     }
 * }
 * ```
 *
 * Java cannot implement a suspending method, so Java middlewares implement
 * [io.cratis.chronicle.java.BlockingReactorMiddleware] instead and are adapted onto this.
 */
interface IReactorMiddleware {
    /**
     * Called immediately before the handler runs.
     *
     * @param context The context of the event being handled.
     * @param event The event itself.
     */
    suspend fun beforeInvoke(context: EventContext, event: Any)

    /**
     * Called after the handler has run, whether it returned or threw.
     *
     * @param context The context of the event that was handled.
     * @param event The event itself.
     */
    suspend fun afterInvoke(context: EventContext, event: Any)
}
