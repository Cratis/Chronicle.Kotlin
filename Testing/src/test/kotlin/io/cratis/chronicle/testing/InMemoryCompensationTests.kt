// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.CompensationFor
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType("ledger-entry-posted")
private data class LedgerEntryPosted(val amount: Long)

@EventType
private data class LedgerNoted(val note: String)

@EventType("ledger-entry-reversed")
@CompensationFor(LedgerEntryPosted::class)
private data class LedgerEntryReversed(val reason: String)

class InMemoryCompensationTests {
    @Test
    fun `a compensation goes to the event source of the original and records the link`() = runBlocking {
        val sequence = InMemoryEventSequence()
        sequence.append("account-1", LedgerEntryPosted(100))

        val result = sequence.compensate(EventSequenceNumber(0), LedgerEntryReversed("wrong account"))

        assertTrue(result.isSuccess)
        assertEquals(1L, result.sequenceNumberValue)
        val compensation = sequence.events.last().context
        assertEquals("account-1", compensation.eventSourceId)
        assertEquals(1L, compensation.sequenceNumber)
        assertNull(sequence.events.first().context.compensationFor)
        assertEquals(0L, compensation.compensationFor)
        assertEquals(2, sequence.count)
    }

    @Test
    fun `compensating an event that does not exist is reported as an error`() = runBlocking {
        val sequence = InMemoryEventSequence()

        val result = sequence.compensate(EventSequenceNumber(5), LedgerEntryReversed("nope"))

        assertFalse(result.isSuccess)
        assertEquals(AppendError.COMPENSATED_EVENT_NOT_FOUND, result.errors.single().message)
        assertEquals(0, sequence.count)
    }

    @Test
    fun `compensating an event of another type is reported as an error`() = runBlocking {
        val sequence = InMemoryEventSequence()
        sequence.append("account-1", LedgerNoted("hello"))

        val result = sequence.compensate(EventSequenceNumber(0), LedgerEntryReversed("wrong type"))

        assertFalse(result.isSuccess)
        assertEquals(AppendError.EVENT_TYPE_DOES_NOT_COMPENSATE_EVENT_TYPE, result.errors.single().message)
        assertEquals(1, sequence.count)
    }

    @Test
    fun `the same event can be compensated more than once`() = runBlocking {
        val sequence = InMemoryEventSequence()
        sequence.append("account-1", LedgerEntryPosted(100))

        assertTrue(sequence.compensate(EventSequenceNumber(0), LedgerEntryReversed("first")).isSuccess)
        assertTrue(sequence.compensate(EventSequenceNumber(0), LedgerEntryReversed("second")).isSuccess)

        assertEquals(listOf<Long?>(null, 0L, 0L), sequence.events.map { it.context.compensationFor })
    }
}
