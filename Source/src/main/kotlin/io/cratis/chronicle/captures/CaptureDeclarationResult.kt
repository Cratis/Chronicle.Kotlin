// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

/**
 * What the kernel made of a capture declaration.
 *
 * A declaration is a piece of text, so the first thing that can go wrong is that the kernel cannot
 * make sense of it. That is an ordinary outcome of writing in a language rather than an exception,
 * and the messages are what you show whoever wrote it - so this is a result rather than a throw.
 */
sealed class CaptureDeclarationResult {
    /** Whether the declaration was accepted. */
    abstract val isSuccess: Boolean

    /** What the kernel had to say. Empty on a clean acceptance. */
    abstract val messages: List<CaptureValidationMessage>

    /**
     * The declaration was accepted.
     *
     * @property capture The capture as the kernel now holds it.
     * @property messages Anything worth knowing that did not stop it being accepted.
     */
    data class Accepted(
        val capture: Capture,
        override val messages: List<CaptureValidationMessage> = emptyList()
    ) : CaptureDeclarationResult() {
        override val isSuccess: Boolean get() = true
    }

    /**
     * The kernel could not accept the declaration, and nothing was saved or started.
     *
     * @property messages What is wrong with it, each naming a line and column.
     */
    data class Rejected(override val messages: List<CaptureValidationMessage>) : CaptureDeclarationResult() {
        override val isSuccess: Boolean get() = false

        override fun toString(): String =
            "The capture declaration was rejected: " + messages.joinToString("; ")
    }
}
