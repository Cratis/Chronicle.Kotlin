// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents the outcome of completing a stream via [IEventSequence.completeStream].
 */
sealed class CompleteStreamResult {
    /**
     * The stream was successfully completed.
     *
     * @property sequenceNumber The tail [EventSequenceNumber] at the moment of completion.
     */
    data class Success(val sequenceNumber: EventSequenceNumber) : CompleteStreamResult()

    /**
     * The default stream — event stream type `"Default"` paired with the default event stream id
     * — can never be completed.
     */
    data object DefaultStreamCannotBeCompleted : CompleteStreamResult()

    /** The stream was already completed and cannot be completed again. */
    data object AlreadyCompleted : CompleteStreamResult()
}
