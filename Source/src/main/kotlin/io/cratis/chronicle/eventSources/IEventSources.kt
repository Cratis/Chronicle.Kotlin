// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/**
 * Defines the event source definitions known to an event store.
 */
interface IEventSources {
    /**
     * All discovered event source definitions.
     *
     * @throws DuplicateEventSourceName Two definitions share a name.
     * @throws DuplicateEventStreamName A definition declares a stream twice.
     */
    val all: List<EventSourceDefinition>

    /**
     * Gets the definition carried by [type].
     *
     * @throws UnknownEventSource The type is not a discovered event source.
     */
    fun getFor(type: KClass<*>): EventSourceDefinition

    /**
     * Gets the definition named [name].
     *
     * @throws UnknownEventSource No discovered definition has that name.
     */
    fun getFor(name: String): EventSourceDefinition

    /**
     * Registers every discovered definition with the kernel. Registration is an upsert; definitions
     * the client no longer declares are retained by the kernel. Does nothing when there are none.
     */
    suspend fun register()

    companion object {
        /**
         * The capability of an event store that does not support event sources. It declares no definitions,
         * registering is a no-op, and anything that needs a definition fails with [UnsupportedOperationException].
         */
        val unsupported: IEventSources = UnsupportedEventSources

        /** An instance with no definitions, for event sequences that are not part of an event store. */
        val none: IEventSources by lazy {
            EventSources("", null, io.cratis.chronicle.artifacts.KnownClientArtifacts.empty)
        }
    }
}
