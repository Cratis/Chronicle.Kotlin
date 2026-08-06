// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class CustomerRegistered(val name: String)

/**
 * These stage operations without ever reaching a kernel - what matters here is what ends up staged,
 * because that is what the composed operation later sends.
 */
class EventSourceOperationsTests {

    @Test
    fun `append stages events in the order they were added`() {
        val operations = EventSourceOperations()

        operations.append(CustomerRegistered("first"))
        operations.append(CustomerRegistered("second"))

        assertEquals(
            listOf(CustomerRegistered("first"), CustomerRegistered("second")),
            operations.getAppendedEvents()
        )
    }

    @Test
    fun `append carries the per-event shaping onto the staged operation`() {
        val occurred = Instant.parse("2020-03-01T10:15:30Z")
        val operations = EventSourceOperations()

        operations.append(
            CustomerRegistered("Jane"),
            eventStreamType = "Onboarding",
            eventStreamId = "2026",
            eventSourceType = "Customer",
            tags = listOf("gdpr"),
            occurred = occurred,
            subject = "person-42"
        )

        val staged = operations.getOperationsOfType<AppendOperation>().single()
        assertEquals("Onboarding", staged.eventStreamType)
        assertEquals("2026", staged.eventStreamId)
        assertEquals("Customer", staged.eventSourceType)
        assertEquals(listOf("gdpr"), staged.tags)
        assertEquals(occurred, staged.occurred)
        assertEquals("person-42", staged.subject)
    }

    @Test
    fun `append leaves the shaping unset so the append falls back to its defaults`() {
        val operations = EventSourceOperations()

        operations.append(CustomerRegistered("Jane"))

        val staged = operations.getOperationsOfType<AppendOperation>().single()
        assertEquals(null, staged.eventStreamType)
        assertEquals(null, staged.eventStreamId)
        assertEquals(null, staged.eventSourceType)
        assertEquals(null, staged.occurred)
        assertEquals(null, staged.subject)
        assertTrue(staged.tags.isEmpty())
    }

    @Test
    fun `an event source starts out unchecked for concurrency`() {
        assertEquals(ConcurrencyScope.notSet, EventSourceOperations().concurrencyScope)
    }

    @Test
    fun `withConcurrencyScope keeps the scope already set when handed notSet`() {
        val scope = ConcurrencyScope(EventSequenceNumber(4), eventSourceId = true)
        val operations = EventSourceOperations().withConcurrencyScope(scope)

        operations.withConcurrencyScope(ConcurrencyScope.notSet)

        // Silently disabling a concurrency check that was explicitly asked for would be the worst
        // possible failure mode here - it would look like it worked.
        assertEquals(scope, operations.concurrencyScope)
    }

    @Test
    fun `withConcurrencyScope replaces an existing scope with a real one`() {
        val replacement = ConcurrencyScope(EventSequenceNumber(9), eventSourceId = true)
        val operations = EventSourceOperations()
            .withConcurrencyScope(ConcurrencyScope(EventSequenceNumber(4), eventSourceId = true))

        operations.withConcurrencyScope(replacement)

        assertEquals(replacement, operations.concurrencyScope)
    }

    @Test
    fun `withConcurrencyScope builds the scope from the builder`() {
        val operations = EventSourceOperations().withConcurrencyScope {
            withSequenceNumber(EventSequenceNumber(7))
            withEventSourceId()
            withEventStreamType("Onboarding")
        }

        assertEquals(
            ConcurrencyScope(EventSequenceNumber(7), eventSourceId = true, eventStreamType = "Onboarding"),
            operations.concurrencyScope
        )
    }
}
