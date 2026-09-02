// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.identity.Identity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnitOfWorkTests {
    private val success = AppendResult(EventSequenceNumber(4), emptyList(), emptyList(), true)

    @Test
    fun `commit makes one append many call preserving order context and complete scopes`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        val events = slot<List<EventForEventSourceId>>()
        val contextSlot = slot<OperationContext>()
        val scopes = slot<Map<String, ConcurrencyScope>>()
        coEvery { sequence.appendMany(capture(events), capture(contextSlot), capture(scopes)) } returns
            listOf(success, success, success)
        val context = OperationContext(UUID.randomUUID(), causedBy = Identity("u1", "User"))
        val firstScope = ConcurrencyScope(EventSequenceNumber(2), eventSourceId = true)
        val secondScope = ConcurrencyScope.noMatchingEvent.copy(eventSourceId = true)
        val unitOfWork = UnitOfWork(sequence, context)

        unitOfWork.append("source-a", "one", AppendOptions(concurrencyScope = firstScope))
        unitOfWork.append("source-b", "two", AppendOptions(concurrencyScope = secondScope))
        unitOfWork.append("source-a", "three", AppendOptions(concurrencyScope = firstScope))
        unitOfWork.commit()

        coVerify(exactly = 1) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
        assertEquals(listOf("one", "two", "three"), events.captured.map { it.event })
        assertEquals(listOf("source-a", "source-b", "source-a"), events.captured.map { it.eventSourceId })
        assertSame(context, contextSlot.captured)
        assertEquals(mapOf("source-a" to firstScope, "source-b" to secondScope), scopes.captured)
        assertTrue(unitOfWork.isCompleted)
        assertTrue(unitOfWork.isSuccess)
    }

    @Test
    fun `rollback and repeated completion are rejected`() = runBlocking {
        val unitOfWork = UnitOfWork(mockk())
        unitOfWork.append("source", "event")
        unitOfWork.rollback()

        assertTrue(unitOfWork.isCompleted)
        assertFalse(unitOfWork.isSuccess)
        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.rollback() } }
        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.commit() } }
        assertThrows(IllegalStateException::class.java) { unitOfWork.append("source", "late") }
    }

    @Test
    fun `failed commit is terminal and cannot partially retry`() {
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } throws IllegalStateException("network")
        val unitOfWork = UnitOfWork(sequence)
        unitOfWork.append("a", "one")
        unitOfWork.append("b", "two")

        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.commit() } }
        assertTrue(unitOfWork.isCompleted)
        assertFalse(unitOfWork.isSuccess)
        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.commit() } }
        coVerify(exactly = 1) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
    }

    @Test
    fun `cancelled commit is terminal`() {
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } throws CancellationException("cancelled")
        val unitOfWork = UnitOfWork(sequence)
        unitOfWork.append("a", "one")

        assertThrows(CancellationException::class.java) { runBlocking { unitOfWork.commit() } }
        assertTrue(unitOfWork.isCompleted)
        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.rollback() } }
    }

    @Test
    fun `empty commit is terminal without an rpc`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        val unitOfWork = UnitOfWork(sequence)

        unitOfWork.commit()

        coVerify(exactly = 0) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
        assertTrue(unitOfWork.isSuccess)
    }

    @Test
    fun `callbacks run in registration order after successful commit and one failure does not stop later callbacks`() {
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } returns listOf(success)
        val unitOfWork = UnitOfWork(sequence)
        val observations = mutableListOf<String>()
        unitOfWork.append("source", "event")
        unitOfWork.onCompleted {
            assertTrue(it.isCompleted)
            assertTrue(it.isSuccess)
            observations.add("first")
        }
        unitOfWork.onCompleted {
            observations.add("throwing")
            throw IllegalArgumentException("callback")
        }
        unitOfWork.onCompleted { observations.add("last") }

        val failure = assertThrows(UnitOfWorkCompletionCallbackException::class.java) {
            runBlocking { unitOfWork.commit() }
        }

        assertEquals(listOf("first", "throwing", "last"), observations)
        assertEquals(listOf("callback"), failure.failures.map { it.message })
        assertTrue(unitOfWork.isCompleted)
        assertTrue(unitOfWork.isSuccess)
        assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.commit() } }
        coVerify(exactly = 1) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
    }

    @Test
    fun `callback registered while commit is in flight waits for terminal state`() = runBlocking {
        val sequence = mockk<IEventSequence>()
        val appendStarted = CompletableDeferred<Unit>()
        val finishAppend = CompletableDeferred<Unit>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } coAnswers {
            appendStarted.complete(Unit)
            finishAppend.await()
            listOf(success)
        }
        val unitOfWork = UnitOfWork(sequence)
        val callbackRan = CompletableDeferred<Boolean>()
        unitOfWork.append("source", "event")

        val commit = launch { unitOfWork.commit() }
        appendStarted.await()
        assertFalse(unitOfWork.isCompleted)
        unitOfWork.onCompleted { callbackRan.complete(it.isCompleted) }
        assertFalse(callbackRan.isCompleted)

        finishAppend.complete(Unit)
        commit.join()

        assertTrue(callbackRan.await())
        assertTrue(unitOfWork.isCompleted)
    }

    @Test
    fun `append failure is terminal callbacks run and callback failure does not mask transport failure`() {
        val transportFailure = IllegalStateException("network")
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } throws transportFailure
        val unitOfWork = UnitOfWork(sequence)
        val observations = mutableListOf<String>()
        unitOfWork.append("source", "event")
        unitOfWork.onCompleted {
            observations.add("first")
            assertTrue(it.isCompleted)
            assertFalse(it.isSuccess)
            throw IllegalArgumentException("callback")
        }
        unitOfWork.onCompleted { observations.add("last") }

        val actual = assertThrows(IllegalStateException::class.java) { runBlocking { unitOfWork.commit() } }

        assertSame(transportFailure, actual)
        assertEquals(listOf("first", "last"), observations)
        assertEquals(1, actual.suppressed.size)
        assertTrue(actual.suppressed.single() is UnitOfWorkCompletionCallbackException)
        coVerify(exactly = 1) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
    }

    @Test
    fun `rollback callbacks observe terminal rolled back state`() = runBlocking {
        val unitOfWork = UnitOfWork(mockk())
        val observations = mutableListOf<Pair<Boolean, Boolean>>()
        unitOfWork.onCompleted { observations.add(it.isCompleted to it.isSuccess) }

        unitOfWork.rollback()

        assertEquals(listOf(true to false), observations)
    }

    @Test
    fun `cancellation is terminal and completion callbacks still run`() {
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        } throws CancellationException("cancelled")
        val unitOfWork = UnitOfWork(sequence)
        var callbackObservedTerminal = false
        unitOfWork.append("source", "event")
        unitOfWork.onCompleted { callbackObservedTerminal = it.isCompleted }

        assertThrows(CancellationException::class.java) { runBlocking { unitOfWork.commit() } }

        assertTrue(callbackObservedTerminal)
        assertTrue(unitOfWork.isCompleted)
        coVerify(exactly = 1) {
            sequence.appendMany(any<List<EventForEventSourceId>>(), any<OperationContext>(), any<Map<String, ConcurrencyScope>>())
        }
    }

    @Test
    fun `unit of work is structurally bound to one event sequence`() {
        val boundSequence = mockk<IEventSequence>()
        val unitOfWork: IUnitOfWork = UnitOfWork(boundSequence)

        assertSame(boundSequence, unitOfWork.eventSequence)
        assertTrue(
            IUnitOfWork::class.java.methods
                .filter { it.name == "append" || it.name == "appendMany" || it.name == "commit" }
                .none { method -> method.parameterTypes.any { IEventSequence::class.java.isAssignableFrom(it) } }
        )
    }
}
