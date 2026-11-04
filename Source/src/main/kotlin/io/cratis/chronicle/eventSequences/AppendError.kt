// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents an error that occurred during an append operation.
 *
 * @property message Human-readable description of the error.
 */
data class AppendError(val message: String) {
    companion object {
        /** The event being compensated for does not exist in the event sequence. */
        const val COMPENSATED_EVENT_NOT_FOUND = "CompensatedEventNotFound"

        /**
         * The event type used for the compensation is not declared as a compensation for the type of the
         * event being compensated.
         */
        const val EVENT_TYPE_DOES_NOT_COMPENSATE_EVENT_TYPE = "EventTypeDoesNotCompensateEventType"
    }

    /** Whether this error says the event being compensated for does not exist. */
    val isCompensatedEventNotFound: Boolean get() = message == COMPENSATED_EVENT_NOT_FOUND

    /** Whether this error says the event type is not declared as a compensation for the compensated event's type. */
    val isEventTypeDoesNotCompensateEventType: Boolean get() = message == EVENT_TYPE_DOES_NOT_COMPENSATE_EVENT_TYPE
}
