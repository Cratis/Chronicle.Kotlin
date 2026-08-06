// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

/**
 * Defines the API surface for the Chronicle client.
 */
interface IChronicleClient : AutoCloseable {
    /**
     * Returns (or creates) the [EventStore] for the given name and optional namespace.
     *
     * @param name The name of the event store.
     * @param namespace The namespace within the event store. Defaults to [EventStoreNamespaceName.default].
     * @return The [EventStore] instance.
     */
    fun getEventStore(
        name: String,
        namespace: String = EventStoreNamespaceName.default.value
    ): EventStore

    /**
     * Lists all event stores known to the kernel — not just the ones cached locally by this client.
     *
     * @return The names of all event stores.
     */
    suspend fun getEventStores(): List<String>

    /**
     * Evicts every cached [EventStore], releasing it from this client's internal cache.
     *
     * Used to release per-event-store subscriptions when the calling environment needs to reset
     * client state without disposing the client itself — for example, between test classes that
     * share an [IChronicleClient] instance.
     */
    fun evictEventStores()

    /** Releases all resources held by this client. */
    fun dispose()

    override fun close() = dispose()
}
