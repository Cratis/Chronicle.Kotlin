// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * A system for working with the observers of an event store.
 */
interface IObservers {
    /**
     * Every observer registered in the event store's current namespace.
     *
     * @return Every observer, with what the event store currently knows about it.
     */
    suspend fun getAll(): List<ObserverInformation>

    /**
     * Removes an observer and everything keyed to it.
     *
     * For the observer whose declaring code is gone - a read model and its projection that were deleted, a
     * reactor that was removed. The observer it registered stays behind, settles into
     * [ObserverRunningState.Disconnected] and keeps its records in the event store forever.
     *
     * Removal covers the whole event store, because an observer's definition is a store-level record. It
     * refuses while the observer is running or has a subscribed client in any namespace, so what it can remove
     * is only ever an observer no client is reporting - stop the declaring application first if you mean to
     * remove a live one. Read model data and sink containers are left untouched.
     *
     * @param observerId The identifier of the observer to remove.
     * @return An [ObserverRemovalResult] describing what happened.
     */
    suspend fun remove(observerId: String): ObserverRemovalResult
}
