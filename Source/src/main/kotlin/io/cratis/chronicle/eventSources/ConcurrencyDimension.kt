// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * A dimension of an append that takes part in the optimistic concurrency check for an event source
 * or stream.
 *
 * Declaring none keeps the append's concurrency behavior exactly as it is without a definition.
 * Declaring one or more makes an append through the definition check only events that match on those
 * dimensions - unless the caller supplies an explicit
 * [io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope], which always wins.
 *
 * @property flag The bit this dimension occupies in the wire representation.
 */
enum class ConcurrencyDimension(val flag: Int) {
    /** The event source id. */
    EventSourceId(1),

    /** The event source type. */
    EventSourceType(2),

    /** The event stream type. */
    EventStreamType(4),

    /** The event stream id. */
    EventStreamId(8)
}

/** Combines dimensions into the wire flag value; no dimensions is `0`. */
internal fun Collection<ConcurrencyDimension>.toFlags(): Int = fold(0) { flags, dimension -> flags or dimension.flag }
