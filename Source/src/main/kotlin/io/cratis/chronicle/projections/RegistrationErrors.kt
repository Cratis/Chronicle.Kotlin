// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val retryAfterKey: Metadata.Key<String> = Metadata.Key.of("retry-after", Metadata.ASCII_STRING_MARSHALLER)

/** The gRPC status this failure carries, or `null` when it is not a gRPC failure. */
internal fun Throwable.grpcStatusCode(): Status.Code? = when (this) {
    is StatusException -> status.code
    is StatusRuntimeException -> status.code
    else -> null
}

/**
 * How long the kernel asked to wait before trying again: set when it answered UNAVAILABLE because it is busy, with the
 * wait, in seconds, in a `retry-after` trailer. `null` for any other failure.
 */
internal fun Throwable.retryAfter(): Duration? {
    if (grpcStatusCode() != Status.Code.UNAVAILABLE) return null
    val seconds = Status.trailersFromThrowable(this)?.get(retryAfterKey)?.toLongOrNull() ?: return null
    return seconds.coerceAtLeast(0).seconds
}
