// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

/**
 * Reports every exception thrown by unit-of-work completion callbacks.
 *
 * The unit of work is already terminal when this is thrown. A committed unit of work remains
 * committed and successful, and its append is never retried. Every registered callback is invoked
 * before the aggregate is reported.
 */
class UnitOfWorkCompletionCallbackException(
    /** Exceptions in callback registration order. */
    val failures: List<Throwable>
) : RuntimeException("${failures.size} unit-of-work completion callback(s) failed") {
    init {
        failures.forEach(::addSuppressed)
    }
}
