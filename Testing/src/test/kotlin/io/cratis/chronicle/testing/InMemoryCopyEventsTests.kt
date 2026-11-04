// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
private data class CopyNamed(val name: String)

@EventType
private data class CopyMoved(val city: String)

class InMemoryCopyEventsTests {
    private suspend fun seeded(): InMemoryEventSequence {
        val sequence = InMemoryEventSequence()
        sequence.append("source", CopyNamed("Ada"))
        sequence.append("other", CopyNamed("Bob"))
        sequence.append("source", CopyMoved("London"))
        return sequence
    }

    private fun InMemoryEventSequence.namesFor(eventSourceId: String) =
        events.filter { it.context.eventSourceId == eventSourceId }.map { it.context.eventType.id.value }

    @Test
    fun `copyForEventSource copies every event type and keeps the originals`() = runBlocking {
        val sequence = seeded()

        sequence.copyForEventSource("source", "target")

        assertEquals(2, sequence.namesFor("target").size)
        assertEquals(2, sequence.namesFor("source").size)
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), sequence.events.map { it.context.sequenceNumber })
    }

    @Test
    fun `copyForEventSource narrows to the given event types`() = runBlocking {
        val sequence = seeded()

        sequence.copyForEventSource("source", "target", listOf(CopyMoved::class))

        assertEquals(1, sequence.namesFor("target").size)
        assertEquals(true, sequence.events.last().content.contains("London"))
    }

    @Test
    fun `copyForEventSource returns a job id`() = runBlocking {
        assertEquals(36, seeded().copyForEventSource("source", "target").length)
    }
}
