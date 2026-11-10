// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/** What an import does when an event in it is invalid. */
enum class OnInvalidEvent {
    /** Commit nothing when any event is invalid. The default. */
    Reject,

    /** Commit the valid events and report the invalid ones as rejections. */
    Skip
}
