// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

/** Whether a capture is currently pulling anything in. */
enum class CaptureStatus {
    /** Saved, but not pulling anything in. */
    Stopped,

    /** Running, and appending events as its source produces them. */
    Started
}
