// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.time.Instant
import java.util.UUID

/**
 * Options that can be supplied when appending events to an event sequence.
 *
 * Every property is optional. Routing is forwarded without client-side defaults: missing or empty
 * routing values are resolved by the kernel; every explicit nonempty value is preserved.
 *
 * The constructor is `@JvmOverloads` so that Java callers keep the shorter positional forms they
 * already compile against. For anything beyond the first argument or two, Java should prefer
 * [io.cratis.chronicle.java.AppendOptionsBuilder] rather than passing nulls positionally.
 *
 * @property correlationId Correlation identifier for this operation.
 *   Defaults to the current [io.cratis.chronicle.correlation.CorrelationIdManager] value.
 * @property concurrencyScope [ConcurrencyScope] to use for concurrency control.
 *   Defaults to [ConcurrencyScope.none], which does not concurrency-check the append.
 * @property eventSourceType The type of the event source. Missing/empty uses the kernel default.
 * @property eventStreamType The type of the event stream. Missing/empty uses the kernel default.
 * @property eventStreamId The identifier of the event stream. Missing/empty uses the kernel default.
 * @property subject The compliance subject this event is about, which is what PII is held against.
 *   Defaults to the event source identifier - set this when the subject is someone other than the
 *   event source.
 * @property tags Tags to attach to the event. Observers can be filtered by tag.
 * @property occurred When the event actually occurred. Defaults to the time the kernel appends it -
 *   set this when importing or backfilling events that happened earlier.
 * @property causation The chain describing what caused this event. Defaults to the ambient chain
 *   held by [io.cratis.chronicle.auditing.CausationManager] for the current thread, which is what
 *   nearly every append should use. Set this only to attribute an append to something other than
 *   the work the current thread is doing - an imported event, or a side effect that belongs to a
 *   chain of its own. An empty list means "no override" and leaves the ambient chain in charge.
 */
data class AppendOptions @JvmOverloads constructor(
    val correlationId: UUID? = null,
    val concurrencyScope: ConcurrencyScope? = null,
    val eventSourceType: String? = null,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val subject: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val causation: List<Causation> = emptyList()
) {
    internal companion object {
        /** Legacy JVM constant retained for compatibility; request construction does not apply it. */
        const val DEFAULT_EVENT_SOURCE_TYPE = "Default"

        /** Legacy JVM constant retained for compatibility; request construction does not apply it. */
        const val DEFAULT_EVENT_STREAM_TYPE = "Default"
    }
}
