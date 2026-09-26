// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * What happened when an event store was asked to remove an observer.
 */
enum class ObserverRemovalOutcome {
    /** The observer and everything keyed to it was removed. */
    Removed,

    /** No observer with that identifier is registered in the event store, so there was nothing to remove. */
    ObserverNotFound,

    /** The observer is running in at least one namespace, so it is still a live observer and cannot be removed. */
    ObserverActive,

    /** A client is still subscribed to the observer in at least one namespace, so its declaring code is still present. */
    ObserverSubscribed
}
