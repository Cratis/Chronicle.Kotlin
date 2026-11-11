// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException

private const val CLIENT_IS_NOT_CONNECTED = "ClientIsNotConnected"

/**
 * Whether an error that ended an observation stream means the kernel no longer knows this client's connection.
 *
 * A restarted kernel, or one that evicted the client, rejects the stream with `ClientIsNotConnected`. Retrying the
 * same stream under the same connection id can never work, so the client has to reconnect as a whole under a new one.
 *
 * Wrapping exceptions are looked through, but only the first gRPC status found decides. `UNAVAILABLE` and `CANCELLED`
 * are transport conditions first: they never count as a lost session, whatever their description says.
 */
internal fun Throwable.isSessionLost(): Boolean {
    val status = generateSequence(this) { error -> error.cause?.takeIf { it !== error } }
        .take(MAX_CAUSE_DEPTH)
        .firstNotNullOfOrNull { error ->
            when (error) {
                is StatusException -> error.status
                is StatusRuntimeException -> error.status
                else -> null
            }
        } ?: return false
    if (status.code == Status.Code.UNAVAILABLE || status.code == Status.Code.CANCELLED) return false
    return status.description?.contains(CLIENT_IS_NOT_CONNECTED) == true
}

private const val MAX_CAUSE_DEPTH = 32
