// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

/**
 * A capture the kernel is holding: something outside Chronicle, pulled in and appended as events.
 *
 * @property id The identifier the capture is held under.
 * @property name Its display name, as the declaration named it.
 * @property declaration The Capture Declaration Language text that defines it.
 * @property status Whether it is currently running.
 */
data class Capture(
    val id: String,
    val name: String,
    val declaration: String,
    val status: CaptureStatus
) {
    /** Whether the capture is currently pulling anything in. */
    val isStarted: Boolean get() = status == CaptureStatus.Started
}
