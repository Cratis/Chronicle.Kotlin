// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * What the event store knows about one of its observers.
 *
 * @property id The identifier of the observer.
 * @property eventSequenceId The event sequence the observer observes.
 * @property type What kind of observer this is.
 * @property runningState The state the observer is currently in.
 * @property lastHandledEventSequenceNumber The position of the last event the observer handled.
 * @property nextEventSequenceNumber The position of the next event the observer expects to handle.
 * @property handledEventCount The total number of events the observer has handled.
 */
data class ObserverInformation(
    val id: String,
    val eventSequenceId: EventSequenceId,
    val type: ObserverType,
    val runningState: ObserverRunningState,
    val lastHandledEventSequenceNumber: EventSequenceNumber,
    val nextEventSequenceNumber: EventSequenceNumber,
    val handledEventCount: Long
)
