// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Thrown when an [IEventSequence] implementation cannot append compensating events.
 *
 * @property implementation The implementation that cannot compensate.
 */
class CompensationsNotSupported(val implementation: Class<*>) : UnsupportedOperationException(
    "Event sequence implementation '${implementation.name}' does not support compensations."
)
