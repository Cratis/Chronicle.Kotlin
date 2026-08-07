// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * What a reactor is told when a replay begins or ends.
 *
 * A replay runs per partition - Chronicle replays each event source independently - so a reactor
 * observing many event sources is notified once per partition, not once overall.
 *
 * @property observerId The observer being replayed.
 * @property partition The event source this replay covers.
 * @property sequenceNumber The position of the event that opened or closed the replay.
 */
data class ReplayContext(
    val observerId: String,
    val partition: String,
    val sequenceNumber: EventSequenceNumber
)
