// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.IReactorMiddleware

/**
 * A reactor middleware written in Java.
 *
 * [IReactorMiddleware] is suspending, which Java cannot implement - a `suspend` method carries a
 * hidden continuation parameter on the JVM. Implement this instead and the client adapts it: a
 * middleware discovered as one of these is wrapped so it takes part in the chain exactly like a
 * Kotlin one.
 *
 * ```java
 * public class HandlerLogging implements BlockingReactorMiddleware {
 *     public void beforeInvoke(EventContext context, Object event) {
 *         System.out.println("handling " + event.getClass().getSimpleName());
 *     }
 *
 *     public void afterInvoke(EventContext context, Object event) {
 *         System.out.println("handled " + event.getClass().getSimpleName());
 *     }
 * }
 * ```
 *
 * The methods block the coroutine they run on, so keep them to work that is genuinely quick -
 * starting a span, setting a logging context, incrementing a counter. Anything that waits on I/O
 * belongs in a Kotlin middleware that can suspend.
 */
interface BlockingReactorMiddleware {
    /**
     * Called immediately before the handler runs.
     *
     * @param context The context of the event being handled.
     * @param event The event itself.
     */
    fun beforeInvoke(context: EventContext, event: Any)

    /**
     * Called after the handler has run, whether it returned or threw.
     *
     * @param context The context of the event that was handled.
     * @param event The event itself.
     */
    fun afterInvoke(context: EventContext, event: Any)
}

/** Presents this Java middleware as an [IReactorMiddleware] so it can join the same chain. */
internal fun BlockingReactorMiddleware.asReactorMiddleware(): IReactorMiddleware =
    object : IReactorMiddleware {
        override suspend fun beforeInvoke(context: EventContext, event: Any) =
            this@asReactorMiddleware.beforeInvoke(context, event)

        override suspend fun afterInvoke(context: EventContext, event: Any) =
            this@asReactorMiddleware.afterInvoke(context, event)
    }
