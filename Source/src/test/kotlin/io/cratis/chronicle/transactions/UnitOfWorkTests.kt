// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.correlation.CorrelationId
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class SomethingHappened(val value: String)

class UnitOfWorkTests {

    private fun eventStoreReturning(sequence: IEventSequence): IEventStore {
        val eventStore = mockk<IEventStore>()
        every { eventStore.getEventSequence(any()) } returns sequence
        return eventStore
    }

    @Test
    fun `is not completed and has no correlation id conflicts by default`() {
        val unitOfWork = UnitOfWork(eventStore = mockk())
        val other = UnitOfWork(eventStore = mockk())

        assertFalse(unitOfWork.isCompleted)
        assertTrue(unitOfWork.isSuccess)
        assertNotEquals(unitOfWork.correlationId, other.correlationId)
    }

    @Test
    fun `uses the supplied correlation id when given one`() {
        val correlationId = CorrelationId.create()
        val unitOfWork = UnitOfWork(correlationId = correlationId, eventStore = mockk())

        assertEquals(correlationId, unitOfWork.correlationId)
    }

    @Test
    fun `addEvent stages the event so getEvents returns it before commit`() {
        val unitOfWork = UnitOfWork(eventStore = mockk())
        val event = SomethingHappened("hello")

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", event)

        assertEquals(listOf(event), unitOfWork.getEvents())
        assertFalse(unitOfWork.isCompleted)
    }

    @Test
    fun `commit appends staged events through the event store's resolved event sequence`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.appendMany("source-1", any(), any()) } returns listOf(
            AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("hello"))
        unitOfWork.commit()

        coVerify(exactly = 1) { sequence.appendMany("source-1", any(), any()) }
        assertTrue(unitOfWork.isCompleted)
        assertTrue(unitOfWork.isSuccess)
    }

    @Test
    fun `commit groups consecutive events for the same event sequence and source into a single appendMany call`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        val batches = mutableListOf<List<Any>>()
        coEvery { sequence.appendMany("source-1", capture(batches), any()) } returns listOf(
            AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true),
            AppendResult(EventSequenceNumber(1), emptyList(), emptyList(), true)
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("b"))
        unitOfWork.commit()

        coVerify(exactly = 1) { sequence.appendMany("source-1", any(), any()) }
        assertEquals(2, batches.single().size)
    }

    @Test
    fun `commit issues a separate appendMany call per distinct event source id`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.appendMany(any(), any(), any()) } returns listOf(
            AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.addEvent(EventSequenceId.eventLog, "source-2", SomethingHappened("b"))
        unitOfWork.commit()

        coVerify(exactly = 1) { sequence.appendMany("source-1", any(), any()) }
        coVerify(exactly = 1) { sequence.appendMany("source-2", any(), any()) }
    }

    @Test
    fun `commit aggregates constraint violations, concurrency violations and append errors from the results`() = runBlocking {
        val violation = ConstraintViolation("unique-email", "already taken")
        val concurrencyViolation = ConcurrencyViolation("source-1", EventSequenceNumber(1), EventSequenceNumber(2))
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.appendMany(any(), any(), any()) } returns listOf(
            AppendResult(
                EventSequenceNumber.unavailable,
                listOf(violation),
                listOf(io.cratis.chronicle.eventSequences.AppendError("boom")),
                false,
                concurrencyViolation
            )
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.commit()

        assertFalse(unitOfWork.isSuccess)
        assertEquals(listOf(violation), unitOfWork.getConstraintViolations())
        assertEquals(listOf(concurrencyViolation), unitOfWork.getConcurrencyViolations())
        assertEquals(1, unitOfWork.getAppendErrors().size)
        assertEquals("boom", unitOfWork.getAppendErrors().single().message)
    }

    @Test
    fun `rollback clears staged events and never touches the event store`() = runBlocking {
        val eventStore = mockk<IEventStore>()
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.rollback()

        assertTrue(unitOfWork.isCompleted)
        assertTrue(unitOfWork.getEvents().isEmpty())
    }

    @Test
    fun `onCompleted supports multiple registrations that are all invoked on commit`() = runBlocking {
        val eventStore = mockk<IEventStore>()
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        var firstCalled = false
        var secondCalled = false
        unitOfWork.onCompleted { firstCalled = true }
        unitOfWork.onCompleted { secondCalled = true }

        unitOfWork.commit()

        assertTrue(firstCalled)
        assertTrue(secondCalled)
    }

    @Test
    fun `onCompleted registrations are all invoked on rollback too`() = runBlocking {
        val eventStore = mockk<IEventStore>()
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        var called = false
        unitOfWork.onCompleted { called = true }

        unitOfWork.rollback()

        assertTrue(called)
    }

    @Test
    fun `tryGetLastCommittedEventSequenceNumber is null before commit and the highest number after`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.appendMany(any(), any(), any()) } returns listOf(
            AppendResult(EventSequenceNumber(3), emptyList(), emptyList(), true),
            AppendResult(EventSequenceNumber(7), emptyList(), emptyList(), true)
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        assertNull(unitOfWork.tryGetLastCommittedEventSequenceNumber())

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("b"))
        unitOfWork.commit()

        assertEquals(EventSequenceNumber(7), unitOfWork.tryGetLastCommittedEventSequenceNumber())
    }

    @Test
    fun `tryGetLastCommittedEventSequenceNumber stays null when commit fails to produce actual sequence numbers`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.appendMany(any(), any(), any()) } returns listOf(
            AppendResult(EventSequenceNumber.unavailable, emptyList(), listOf(io.cratis.chronicle.eventSequences.AppendError("boom")), false)
        )
        val eventStore = eventStoreReturning(sequence)
        val unitOfWork = UnitOfWork(eventStore = eventStore)

        unitOfWork.addEvent(EventSequenceId.eventLog, "source-1", SomethingHappened("a"))
        unitOfWork.commit()

        assertNull(unitOfWork.tryGetLastCommittedEventSequenceNumber())
    }
}
