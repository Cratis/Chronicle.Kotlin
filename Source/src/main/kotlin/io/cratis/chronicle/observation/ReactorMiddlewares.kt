// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext

/**
 * The middleware chain wrapped around every reactor handler invocation.
 *
 * @property middlewares The middlewares to run, outermost first.
 */
class ReactorMiddlewares(private val middlewares: List<IReactorMiddleware>) {
    /**
     * Runs [handler] with every middleware wrapped around it.
     *
     * `beforeInvoke` runs in order and `afterInvoke` in reverse, so the middlewares nest rather than
     * queue: the first to open something is the last to close it.
     *
     * Only the middlewares whose `beforeInvoke` actually ran are unwound. A middleware that throws on
     * the way in never got the chance to open anything, and neither did any that would have followed
     * it, so calling their `afterInvoke` would be asking them to close something that was never
     * opened. Unwinding is otherwise unconditional - the handler throwing must not leave a span open
     * or a logging context set.
     */
    suspend fun invoke(context: EventContext, event: Any, handler: suspend () -> Any?): Any? {
        if (middlewares.isEmpty()) return handler()

        var entered = 0
        try {
            for (middleware in middlewares) {
                middleware.beforeInvoke(context, event)
                entered++
            }
            return handler()
        } finally {
            for (index in entered - 1 downTo 0) {
                middlewares[index].afterInvoke(context, event)
            }
        }
    }

    companion object {
        /** No middlewares at all, which is what a client that declares none uses. */
        val none: ReactorMiddlewares = ReactorMiddlewares(emptyList())
    }
}
