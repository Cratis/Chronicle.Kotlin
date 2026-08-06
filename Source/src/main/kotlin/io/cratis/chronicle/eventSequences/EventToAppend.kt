// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import java.time.Instant

/**
 * An event together with everything needed to place it in an event sequence.
 *
 * The single-event-source [IEventSequence.appendMany] shapes every event in the batch the same way,
 * which is all a batch about one thing needs. This is what the multi-event-source overload takes
 * instead: each event carries its own event source id and its own shaping, so one atomic batch can
 * span many event sources and many streams.
 *
 * The constructor is `@JvmOverloads` so Java can construct the common short forms positionally
 * without passing a run of nulls.
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
 */
data class EventToAppend @JvmOverloads constructor(
    val eventSourceId: String,
    val event: Any,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val subject: String? = null
)
