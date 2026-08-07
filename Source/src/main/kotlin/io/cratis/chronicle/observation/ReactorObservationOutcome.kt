// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * How far a reactor got through a batch of events, and what went wrong if anything did.
 *
 * This is what the client answers the kernel with. [lastSuccessfulSequenceNumber] is the position
 * the reactor genuinely got past, so an observation that failed part-way resumes from the event that
 * failed rather than from wherever the batch happened to end.
 */
internal class ReactorObservationOutcome {
    private val messages = mutableListOf<String>()

    /** The last position observed without error. */
    var lastSuccessfulSequenceNumber: Long = 0L
        private set

    /** The stack trace of the most recent failure, empty when nothing failed. */
    var stackTrace: String = ""
        private set

    /** The error messages collected across the batch. */
    val exceptions: List<String> get() = messages

    /** Whether the whole batch was observed without error. */
    val isSuccess: Boolean get() = messages.isEmpty()

    /** Records that the event at [sequenceNumber] was observed. */
    fun observed(sequenceNumber: Long) {
        lastSuccessfulSequenceNumber = sequenceNumber
    }

    /**
     * Records that [handlerName] failed with [error].
     *
     * Reflection reports a throwing handler as an InvocationTargetException with no message of its
     * own, so both the message and the stack trace are taken from what the handler actually raised -
     * otherwise every stuck partition would read "Error in someHandler".
     */
    fun failed(error: Exception, handlerName: String) {
        messages.add(error.messageFor(handlerName))
        stackTrace = error.unwrapReflectionFailure().stackTraceToString()
    }
}
