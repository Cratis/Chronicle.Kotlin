// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class SideEffectHappened(val value: String)

/**
 * One type carries an event and its event source for both a batch append and a reactor side effect,
 * matching the C# client. These pin the parts most likely to drift back apart: the defaults, and
 * that the shaping survives the single-event path rather than being silently dropped.
 */
class EventForEventSourceIdTests {

    @Test
    fun `only the event source and event are required`() {
        val entry = EventForEventSourceId("source-1", SideEffectHappened("hello"))

        assertEquals("source-1", entry.eventSourceId)
        assertNull(entry.eventStreamType)
        assertNull(entry.eventStreamId)
        assertNull(entry.eventSourceType)
        assertNull(entry.subject)
        assertNull(entry.occurred)
        assertTrue(entry.tags.isEmpty())
        assertTrue(entry.causation.isEmpty())
    }

    @Test
    fun `shaping survives conversion to append options`() {
        val occurred = Instant.parse("2020-03-01T10:15:30Z")
        val options = EventForEventSourceId(
            eventSourceId = "visit-1",
            event = SideEffectHappened("hello"),
            eventStreamType = "Onboarding",
            eventStreamId = "stream-9",
            eventSourceType = "Patient",
            tags = listOf("gdpr"),
            occurred = occurred,
            subject = "patient-42",
            causation = listOf(Causation(occurred, CausationType("Import")))
        ).toAppendOptions()

        assertEquals("Onboarding", options.eventStreamType)
        assertEquals("stream-9", options.eventStreamId)
        assertEquals("Patient", options.eventSourceType)
        assertEquals(listOf("gdpr"), options.tags)
        assertEquals(occurred, options.occurred)
        assertEquals("patient-42", options.subject)
        assertEquals(listOf(Causation(occurred, CausationType("Import"))), options.causation)
    }

    @Test
    fun `an unshaped entry converts to options that change nothing`() {
        // A reactor returning a bare event must keep appending exactly as it did before the type
        // grew these fields.
        val options = EventForEventSourceId("source-1", SideEffectHappened("hello")).toAppendOptions()

        assertNull(options.eventStreamType)
        assertNull(options.eventStreamId)
        assertNull(options.eventSourceType)
        assertNull(options.subject)
        assertNull(options.occurred)
        assertTrue(options.tags.isEmpty())
        assertTrue(options.causation.isEmpty())
    }
}
