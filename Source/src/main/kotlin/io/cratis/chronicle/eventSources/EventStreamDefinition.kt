// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * The definition of a stream within an [EventSourceDefinition].
 *
 * @property name The stable name of the stream.
 * @property description A human readable description of the stream.
 * @property concurrency The dimensions that take part in concurrency checks for the stream; empty falls
 *   back to the event source's.
 */
data class EventStreamDefinition(
    val name: String,
    val description: String = "",
    val concurrency: Set<ConcurrencyDimension> = emptySet()
)
