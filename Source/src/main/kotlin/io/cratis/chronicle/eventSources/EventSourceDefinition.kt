// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/**
 * The definition of a registered event source.
 *
 * @property type The class that carries the definition.
 * @property name The stable name of the event source.
 * @property description A human readable description of the event source.
 * @property concurrency The default dimensions that take part in concurrency checks.
 * @property streams The declared streams.
 */
data class EventSourceDefinition(
    val type: KClass<*>,
    val name: String,
    val description: String = "",
    val concurrency: Set<ConcurrencyDimension> = emptySet(),
    val streams: List<EventStreamDefinition> = emptyList()
) {
    /** Finds a declared stream by [name], or `null` when the event source does not declare it. */
    fun findStream(name: String): EventStreamDefinition? = streams.firstOrNull { it.name == name }

    /**
     * Gets the dimensions that apply to an append: the [stream]'s when it declares any, otherwise the
     * event source's.
     */
    fun concurrencyFor(stream: EventStreamDefinition?): Set<ConcurrencyDimension> =
        if (stream != null && stream.concurrency.isNotEmpty()) stream.concurrency else concurrency
}
