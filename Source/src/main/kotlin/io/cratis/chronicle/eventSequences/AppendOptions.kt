// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.time.Instant

/**
 * Options that can be supplied when appending events to an event sequence.
 *
 * These options shape an event and its concurrency check. Correlation, causation, and identity
 * belong to the explicit [io.cratis.chronicle.OperationContext] supplied to the append operation.
 *
 * The constructor is `@JvmOverloads` so that Java callers keep the shorter positional forms they
 * already compile against. For anything beyond the first argument or two, Java should prefer
 * [io.cratis.chronicle.java.AppendOptionsBuilder] rather than passing nulls positionally.
 *
 * @property concurrencyScope [ConcurrencyScope] to use for concurrency control.
 *   Defaults to [ConcurrencyScope.none], which does not concurrency-check the append.
 * @property eventSourceType The type of the event source. Defaults to `Default`.
 * @property eventStreamType The type of the event stream to append to. Defaults to `Default`.
 * @property eventStreamId The identifier of the event stream to append to.
 *   Defaults to the event source identifier.
 * @property subject The compliance subject this event is about, which is what PII is held against.
 *   Defaults to the event source identifier - set this when the subject is someone other than the
 *   event source.
 * @property tags Tags to attach to the event. Observers can be filtered by tag.
 * @property occurred When the event actually occurred. Defaults to the time the kernel appends it -
 *   set this when importing or backfilling events that happened earlier.
 */
data class AppendOptions @JvmOverloads constructor(
    val concurrencyScope: ConcurrencyScope? = null,
    val eventSourceType: String? = null,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val subject: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null
) {
    internal companion object {
        /** The event source type used when none is specified. */
        const val DEFAULT_EVENT_SOURCE_TYPE = "Default"

        /** The event stream type used when none is specified. */
        const val DEFAULT_EVENT_STREAM_TYPE = "Default"
    }
}
