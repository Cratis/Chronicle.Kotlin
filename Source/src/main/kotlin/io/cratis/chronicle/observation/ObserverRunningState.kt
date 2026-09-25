// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * The state an observer reported on [ObserverInformation] is in.
 */
enum class ObserverRunningState {
    /** The state is not known. */
    Unknown,

    /** The observer is running and has a subscribed client. */
    Active,

    /** The observer is temporarily suspended. */
    Suspended,

    /** The observer is replaying. */
    Replaying,

    /** No client is currently subscribed to the observer. */
    Disconnected,

    /** The observer is quarantined after repeated failures. */
    Quarantined
}
