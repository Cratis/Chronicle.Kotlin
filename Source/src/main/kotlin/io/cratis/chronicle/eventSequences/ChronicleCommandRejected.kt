// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Thrown when the kernel rejects a command outright - unauthorized, or failed with an unhandled
 * exception - rather than returning a normal, checkable result such as a constraint or concurrency
 * violation on [AppendResult].
 */
class ChronicleCommandRejected(operation: String, reason: String) : RuntimeException(
    "Chronicle rejected '$operation': $reason"
)
