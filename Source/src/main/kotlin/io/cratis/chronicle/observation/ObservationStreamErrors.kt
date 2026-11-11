// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.grpc.StatusException
import io.grpc.StatusRuntimeException

private const val CLIENT_IS_NOT_CONNECTED = "ClientIsNotConnected"

/**
 * Whether an error that ended an observation stream means the kernel no longer knows this client's connection.
 *
 * A restarted kernel, or one that evicted the client, rejects the stream with `ClientIsNotConnected`. Retrying the
 * same stream under the same connection id can never work, so the client has to reconnect as a whole under a new one.
 * Wrapping exceptions are looked through.
 */
internal fun Throwable.isSessionLost(): Boolean =
    generateSequence(this) { error -> error.cause?.takeIf { it !== error } }
        .take(MAX_CAUSE_DEPTH)
        .any { error ->
            val description = when (error) {
                is StatusException -> error.status.description
                is StatusRuntimeException -> error.status.description
                else -> null
            }
            description?.contains(CLIENT_IS_NOT_CONNECTED) == true ||
                ((error is StatusException || error is StatusRuntimeException) && error.message?.contains(CLIENT_IS_NOT_CONNECTED) == true)
        }

private const val MAX_CAUSE_DEPTH = 32
