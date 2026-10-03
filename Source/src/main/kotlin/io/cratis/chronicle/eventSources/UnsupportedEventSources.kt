// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/** The [IEventSources] of an event store implementation that predates, or opts out of, event sources. */
internal object UnsupportedEventSources : IEventSources {
    private const val MESSAGE = "This event store does not support event sources."

    override val all: List<EventSourceDefinition> get() = emptyList()

    override fun getFor(type: KClass<*>): EventSourceDefinition = throw UnsupportedOperationException(MESSAGE)

    override fun getFor(name: String): EventSourceDefinition = throw UnsupportedOperationException(MESSAGE)

    override suspend fun register() = Unit
}
