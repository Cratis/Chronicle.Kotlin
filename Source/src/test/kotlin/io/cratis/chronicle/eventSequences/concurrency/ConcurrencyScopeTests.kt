// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.concurrency

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConcurrencyScopeTests {

    @Test
    fun `none applies no constraints and is not incomplete`() {
        assertEquals(EventSequenceNumber.unavailable, ConcurrencyScope.none.sequenceNumber)
        assertFalse(ConcurrencyScope.none.isIncomplete)
    }

    @Test
    fun `notSet has no actual sequence number and is not incomplete`() {
        assertEquals(EventSequenceNumber.max, ConcurrencyScope.notSet.sequenceNumber)
        assertFalse(ConcurrencyScope.notSet.isIncomplete)
    }

    @Test
    fun `a scope without an actual sequence number that is neither none nor notSet is incomplete`() {
        val scope = ConcurrencyScope(EventSequenceNumber.unavailable, eventSourceId = true)
        assertTrue(scope.isIncomplete)
    }

    @Test
    fun `a scope with an actual sequence number is never incomplete`() {
        val scope = ConcurrencyScope(EventSequenceNumber(5))
        assertFalse(scope.isIncomplete)
    }

    @Test
    fun `builder captures every configured dimension`() {
        val eventType = EventTypeDescriptor(EventTypeId("some-event"))

        val scope = ConcurrencyScopeBuilder()
            .withSequenceNumber(EventSequenceNumber(42))
            .withEventSourceId()
            .withEventStreamType("Onboarding")
            .withEventStreamId("2026")
            .withEventSourceType("Account")
            .withEventType(eventType)
            .build()

        assertEquals(EventSequenceNumber(42), scope.sequenceNumber)
        assertTrue(scope.eventSourceId)
        assertEquals("Onboarding", scope.eventStreamType)
        assertEquals("2026", scope.eventStreamId)
        assertEquals("Account", scope.eventSourceType)
        assertEquals(listOf(eventType), scope.eventTypes)
    }

    @Test
    fun `builder deduplicates event types`() {
        val eventType = EventTypeDescriptor(EventTypeId("some-event"))

        val scope = ConcurrencyScopeBuilder()
            .withSequenceNumber(EventSequenceNumber(1))
            .withEventType(eventType)
            .withEventType(eventType)
            .build()

        assertEquals(1, scope.eventTypes.size)
    }

    @Test
    fun `builder rejects sequence number after expects no matching event`() {
        val builder = ConcurrencyScopeBuilder().expectsNoMatchingEvent()

        assertThrows(IllegalStateException::class.java) {
            builder.withSequenceNumber(EventSequenceNumber(5))
        }
    }

    @Test
    fun `builder rejects expects no matching event after sequence number`() {
        val builder = ConcurrencyScopeBuilder().withSequenceNumber(EventSequenceNumber(5))

        assertThrows(IllegalStateException::class.java) {
            builder.expectsNoMatchingEvent()
        }
    }

    @Test
    fun `builder defaults to unavailable sequence number and no narrowing`() {
        val scope = ConcurrencyScopeBuilder().build()

        assertEquals(EventSequenceNumber.unavailable, scope.sequenceNumber)
        assertFalse(scope.eventSourceId)
        assertEquals(emptyList<EventTypeDescriptor>(), scope.eventTypes)
    }
}
