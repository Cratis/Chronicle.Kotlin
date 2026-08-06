// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import java.time.Instant

/**
 * An event staged for appending by [IEventSourceOperations.append].
 *
 * This is [io.cratis.chronicle.eventSequences.EventForEventSourceId] without the event source id, which the
 * enclosing [IEventSourceOperations] already establishes for every event staged against it.
 *
 * @property event The event object to append. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
 * @property eventStreamType The type of the event stream to append to. Defaults to `Default`.
 * @property eventStreamId The identifier of the event stream to append to. Defaults to the event source id.
 * @property eventSourceType The type of the event source. Defaults to `Default`.
 * @property tags Tags to attach to the event. Observers can be filtered by tag.
 * @property occurred When the event actually occurred. Defaults to the time the kernel appends it.
 * @property subject The compliance subject this event is about. Defaults to the event source id.
 */
data class AppendOperation @JvmOverloads constructor(
    val event: Any,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val subject: String? = null
) : IEventSequenceOperation
