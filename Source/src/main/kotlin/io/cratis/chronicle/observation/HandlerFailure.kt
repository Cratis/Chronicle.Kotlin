// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.lang.reflect.InvocationTargetException

/**
 * The failure a handler actually raised, rather than the one reflection wrapped it in.
 *
 * Handlers are invoked reflectively, and reflection reports a throwing method as an
 * [InvocationTargetException] whose own message is `null`. Reported as-is, every failure reaching
 * the kernel would read "Error in someHandler" - which is exactly the message someone looking at a
 * stuck partition does not need. Unwrapping gives them the message and the stack trace the handler
 * raised.
 */
internal fun Throwable.unwrapReflectionFailure(): Throwable =
    if (this is InvocationTargetException) targetException ?: this else this

/** What to report for a failure in [handlerName], preferring the handler's own message. */
internal fun Throwable.messageFor(handlerName: String): String {
    val actual = unwrapReflectionFailure()
    return actual.message?.ifBlank { null }
        ?: "${actual::class.simpleName ?: "Error"} in $handlerName"
}
