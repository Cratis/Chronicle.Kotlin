// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * The result of fast forwarding an observer.
 *
 * @property outcome What happened.
 * @property previousNextEventSequenceNumber The next event the observer expected before the fast forward.
 * @property nextEventSequenceNumber The next event the observer expects after the fast forward.
 */
data class FastForwardResult(
    val outcome: FastForwardOutcome,
    val previousNextEventSequenceNumber: EventSequenceNumber,
    val nextEventSequenceNumber: EventSequenceNumber
)
