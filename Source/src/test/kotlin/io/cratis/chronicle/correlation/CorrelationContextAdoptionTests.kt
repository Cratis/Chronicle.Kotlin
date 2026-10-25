// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.toBclGuid
import io.cratis.fundamentals.execution.CorrelationId
import io.cratis.fundamentals.execution.CorrelationIdContext
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EventType(generation = 1)
private data class AdoptionEvent(val value: String)

class CorrelationContextAdoptionTests {
    private val shared = CorrelationIdContext.shared()

    @BeforeEach
    @AfterEach
    fun reset() {
        shared.clear()
    }

    @Test
    fun `core shared context write is read through the chronicle singleton`() {
        val id = UUID.randomUUID()
        shared.set(CorrelationId(id))
        assertEquals(id, correlationIdManager.current)
    }

    @Test
    fun `chronicle singleton write is read through the core shared context`() {
        val id = UUID.randomUUID()
        correlationIdManager.set(id)
        assertEquals(CorrelationId(id), shared.current)
    }

    @Test
    fun `clear through either side yields the same next lazy value`() {
        val id = UUID.randomUUID()
        correlationIdManager.set(id)
        shared.clear()
        val generated = correlationIdManager.current
        assertNotEquals(id, generated)
        assertEquals(CorrelationId(generated), shared.current)
        correlationIdManager.clear()
        val next = shared.current
        assertNotEquals(CorrelationId(generated), next)
        assertEquals(next.value(), correlationIdManager.current)
    }

    @Test
    fun `explicit zero uuid is visible through both apis`() {
        val zero = UUID(0L, 0L)
        correlationIdManager.set(zero)
        assertEquals(zero, correlationIdManager.current)
        assertEquals(CorrelationId.NOT_SET, shared.current)
        shared.clear()
        shared.set(CorrelationId.NOT_SET)
        assertEquals(zero, correlationIdManager.current)
    }

    @Test
    fun `constructed managers stay independent of each other and of the singleton`() {
        val first = CorrelationIdManager()
        val second = CorrelationIdManager()
        val id = UUID.randomUUID()
        first.set(id)
        assertNotEquals(id, second.current)
        assertNotEquals(id, correlationIdManager.current)
        assertNotEquals(id, shared.current.value())
        assertSame(correlationIdManager, JavaCorrelationUsage.singleton())
        assertEquals(id, JavaCorrelationUsage.roundTrip(first, id))
    }

    @Test
    fun `worker threads do not inherit the shared binding`() {
        val id = UUID.randomUUID()
        correlationIdManager.set(id)
        val executor = Executors.newSingleThreadExecutor()
        try {
            assertNotEquals(id, executor.submit<UUID> { correlationIdManager.current }.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `append falls back to the shared context and explicit options override it without changing it`() = runBlocking {
        val request = slot<Sequences.AppendRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(request), any()) } returns Sequences.CommandResult_AppendResponse.newBuilder()
            .setIsAuthorized(true).setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(1)).build()
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)

        val ambient = CorrelationId(UUID.randomUUID())
        shared.set(ambient)
        sequence.append("source-1", AdoptionEvent("a"))
        assertEquals(ambient.value().toBclGuid(), request.captured.correlationId)

        val explicit = UUID.randomUUID()
        sequence.append("source-1", AdoptionEvent("b"), AppendOptions(correlationId = explicit))
        assertEquals(explicit.toBclGuid(), request.captured.correlationId)
        assertEquals(ambient, shared.current)
    }
}
