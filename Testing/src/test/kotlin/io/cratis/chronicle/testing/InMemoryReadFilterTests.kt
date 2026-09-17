// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
private data class ReadFilterEvent(val marker: String)

/**
 * The server double's read has to narrow the way the kernel narrows, or a spec passes here and the
 * same code returns nothing in production.
 *
 * The case that makes this worth a spec: since the kernel owns append routing, an append with no
 * routing options is stored on stream type `All`, so a read narrowed to the legacy `Default` stream
 * type finds none of it. A double that ignored the filter would answer with the events anyway and
 * hide the very mistake this release's callers are most likely to make.
 */
class InMemoryReadFilterTests {
    private val defaulted = "defaulted"
    private val routed = "routed"

    @Test
    fun `an unnarrowed read returns events the kernel routed by default`() = runBlocking {
        val sequence = sequenceWithBothRoutes()

        assertEquals(listOf(defaulted, routed), sequence.markersOf())
        assertEquals(listOf(defaulted, routed), sequence.markersOf(eventStreamType = "", eventStreamId = ""))
    }

    @Test
    fun `narrowing to the legacy route no longer finds kernel defaulted events`() = runBlocking {
        val sequence = sequenceWithBothRoutes()

        assertEquals(emptyList<String>(), sequence.markersOf(eventStreamType = "Default"))
        assertEquals(listOf(defaulted), sequence.markersOf(eventStreamType = "All", eventStreamId = "Default"))
    }

    @Test
    fun `narrowing to an explicit route returns only what was appended to it`() = runBlocking {
        val sequence = sequenceWithBothRoutes()

        assertEquals(listOf(routed), sequence.markersOf(eventStreamType = "Orders", eventStreamId = "2026"))
        assertEquals(emptyList<String>(), sequence.markersOf(eventStreamType = "Orders", eventStreamId = "2027"))
    }

    @Test
    fun `an event source type filter narrows nothing because the kernel query has no such filter`() = runBlocking {
        val sequence = sequenceWithBothRoutes()

        assertEquals(listOf(defaulted, routed), sequence.markersOf(eventSourceType = "Account"))
        assertEquals(listOf(defaulted, routed), sequence.markersOf(eventSourceType = "Nonexistent"))
    }

    private suspend fun sequenceWithBothRoutes(): InMemoryEventSequence {
        val sequence = InMemoryEventSequence()
        sequence.append("source-1", ReadFilterEvent(defaulted))
        sequence.append(
            "source-1",
            ReadFilterEvent(routed),
            AppendOptions(eventSourceType = "Account", eventStreamType = "Orders", eventStreamId = "2026")
        )
        return sequence
    }

    private suspend fun InMemoryEventSequence.markersOf(
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null
    ): List<String> = getForEventSourceIdAndEventTypes(
        "source-1",
        listOf(ReadFilterEvent::class),
        eventStreamType,
        eventStreamId,
        eventSourceType
    ).map { chronicleMarkerOf(it.content) }

    private fun chronicleMarkerOf(content: String): String =
        Regex("\"marker\"\\s*:\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1)
            ?: error("no marker in $content")
}
