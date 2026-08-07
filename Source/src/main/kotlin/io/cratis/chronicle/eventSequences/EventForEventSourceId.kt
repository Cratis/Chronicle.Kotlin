// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.auditing.Causation
import java.time.Instant

/**
 * An event together with the event source it belongs to, and how it should be placed in a sequence.
 *
 * This serves both places an event needs to name its own event source:
 *
 * - Return it (or a `List` of it) from a [io.cratis.chronicle.observation.Reactor] handler to append
 *   a side effect against an event source other than the one that triggered the reactor.
 * - Pass a collection of them to [IEventSequence.appendMany] to commit one atomic batch spanning
 *   many event sources and many streams.
 *
 * The C# client uses one type for both, carrying the same fields, so an example written against
 * either client reads the same way.
 *
 * Everything past [event] is optional and falls back to the same default a plain append uses.
 *
 * The constructor is `@JvmOverloads` so Java can construct the short forms positionally rather than
 * passing a run of nulls. Beyond the first field or two, prefer named arguments in Kotlin.
 *
 * @property eventSourceId The identifier of the event source to append [event] to.
 * @property event The event object to append. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
 * @property eventStreamType The type of the event stream to append to. Defaults to `Default`.
 * @property eventStreamId The identifier of the event stream to append to. Defaults to [eventSourceId].
 * @property eventSourceType The type of the event source. Defaults to `Default`.
 * @property tags Tags to attach to the event. Observers can be filtered by tag.
 * @property occurred When the event actually occurred. Defaults to the time the kernel appends it -
 *   set this when importing or backfilling events that happened earlier.
 * @property subject The compliance subject this event is about, which is what PII is held against.
 *   Defaults to [eventSourceId] - set this when the subject is someone other than the event source.
 * @property causation The chain describing what caused this event, overriding the ambient one. This
 *   is what lets a reactor side effect attribute itself to something other than the chain the
 *   triggering event left on the thread. Note that the kernel carries one chain per
 *   [IEventSequence.appendMany] batch rather than one per event, so a batch whose events disagree
 *   on causation cannot be expressed and is rejected rather than having the difference dropped.
 */
data class EventForEventSourceId @JvmOverloads constructor(
    val eventSourceId: String,
    val event: Any,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val subject: String? = null,
    val causation: List<Causation> = emptyList()
) {
    /**
     * The shaping expressed as [AppendOptions], for the paths that append one event at a time -
     * reactor side effects in particular, which would otherwise silently discard everything past
     * [eventSourceId] and [event].
     */
    internal fun toAppendOptions(): AppendOptions = AppendOptions(
        eventSourceType = eventSourceType,
        eventStreamType = eventStreamType,
        eventStreamId = eventStreamId,
        subject = subject,
        tags = tags,
        occurred = occurred,
        causation = causation
    )
}
