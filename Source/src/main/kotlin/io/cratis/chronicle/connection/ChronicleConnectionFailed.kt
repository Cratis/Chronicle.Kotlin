// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/**
 * Registration could not begin because the connection was rejected. The client keeps retrying;
 * a later acknowledged connection clears the failure so subsequent calls may proceed.
 */
class ChronicleConnectionFailed(cause: Throwable) : RuntimeException(
    "Chronicle connection failed: ${cause.message}. The client keeps retrying; correct the credentials or server compatibility and try again.",
    cause
)
