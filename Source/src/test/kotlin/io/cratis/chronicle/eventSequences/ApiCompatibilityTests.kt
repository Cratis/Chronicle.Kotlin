// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

private class SomeSource

/** Callers written before event sources existed: the old constructor and `copy` shapes must keep working. */
class ApiCompatibilityTests {
    private val context = EventContext(
        sequenceNumber = 1,
        eventSourceId = "id",
        eventType = EventTypeDescriptor(EventTypeId("E"), EventTypeGeneration(1)),
        occurred = Instant.EPOCH,
        correlationId = UUID.randomUUID(),
        causedBy = Identity.unknown,
        eventSource = "Account"
    )

    @Test
    fun `the sixteen argument event context constructor still works`() {
        val legacy = EventContext(
            1, "id", EventTypeDescriptor(EventTypeId("E"), EventTypeGeneration(1)), Instant.EPOCH, UUID.randomUUID(),
            Identity.unknown, "a", "b", "c", "store", "ns", emptyList(), listOf("t"), "h", EventObservationState.none, "subject"
        )

        assertEquals("", legacy.eventSource)
        assertEquals("subject", legacy.subject)
    }

    @Test
    fun `copying an event context the old way keeps the event source`() {
        val copied = context.copy(
            context.sequenceNumber, context.eventSourceId, context.eventType, context.occurred, context.correlationId,
            context.causedBy, "a", "b", "c", "store", "ns", emptyList(), emptyList(), "h", EventObservationState.none, "s"
        )

        assertEquals("Account", copied.eventSource)
        assertEquals("ns", copied.namespace)
    }

    @Test
    fun `copying an event context by name keeps the event source`() {
        assertEquals("Account", context.copy(namespace = "other").eventSource)
    }

    @Test
    fun `copying append options the old way keeps the routing`() {
        val options = AppendOptions(eventSource = SomeSource::class, eventStream = "Stream")

        val copied = options.copy(null, null, "Type", null, null, null, emptyList(), null, emptyList())

        assertEquals(SomeSource::class, copied.eventSource)
        assertEquals("Stream", copied.eventStream)
        assertEquals("Type", copied.eventSourceType)
    }

    @Test
    fun `the nine argument append options constructor leaves routing unset`() {
        val options = AppendOptions(null, null, null, null, null, null, emptyList(), null, emptyList())

        assertNull(options.eventSource)
        assertNull(options.eventStream)
    }

    @Test
    fun `copying an event for event source id the old way keeps the routing`() {
        val event = EventForEventSourceId("id", Any(), eventSource = SomeSource::class, eventStream = "Stream")

        val copied = event.copy("other", event.event, null, null, null, emptyList(), null, null, emptyList())

        assertEquals("other", copied.eventSourceId)
        assertEquals(SomeSource::class, copied.eventSource)
        assertEquals("Stream", copied.eventStream)
    }

    @Test
    fun `the nine argument event for event source id constructor leaves routing unset`() {
        val event = EventForEventSourceId("id", Any(), null, null, null, emptyList(), null, null, emptyList())

        assertNull(event.eventSource)
    }
}
