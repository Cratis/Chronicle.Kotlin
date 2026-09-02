// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@EventType
private data class ConcurrencyContractEvent(val value: String)

class ConcurrencyContractTests {
    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "store", "namespace", stub)

    @Test
    fun `wire sentinels match 16 44 1`() {
        assertEquals(-1L, EventSequenceNumber.unavailable.value)
        assertEquals(-2L, EventSequenceNumber.max.value)
        assertEquals(-3L, EventSequenceNumber.beforeFirst.value)
    }

    @ParameterizedTest
    @ValueSource(longs = [-1L, -2L, -3L, 9223372036854775807L])
    fun `single append preserves every reserved sentinel and positive long max value`(wireValue: Long) = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } returns Eventsequences.AppendResponse.newBuilder()
            .setSequenceNumber(wireValue)
            .build()

        val result = sequenceFor(stub).append("source", ConcurrencyContractEvent("one"))

        assertEquals(wireValue, result.sequenceNumber.value)
    }

    @ParameterizedTest
    @ValueSource(longs = [-1L, -2L, -3L, 9223372036854775807L])
    fun `batch append preserves every reserved sentinel and positive long max value`(wireValue: Long) = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(any(), any()) } returns Eventsequences.AppendManyResponse.newBuilder()
            .addSequenceNumbers(wireValue)
            .build()

        val result = sequenceFor(stub).appendMany("source", listOf(ConcurrencyContractEvent("one"))).single()

        assertEquals(wireValue, result.sequenceNumber.value)
    }

    @Test
    fun `expects no matching event uses the dedicated wire flag not before first as an expected number`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(request), any()) } returns Eventsequences.AppendResponse.getDefaultInstance()

        sequenceFor(stub).append(
            "source",
            ConcurrencyContractEvent("one"),
            OperationContext.system(),
            AppendOptions(concurrencyScope = ConcurrencyScope.noMatchingEvent.copy(eventSourceId = true))
        )

        assertTrue(request.captured.concurrencyScope.expectsNoMatchingEvent)
        assertEquals(-1L, request.captured.concurrencyScope.sequenceNumber)
    }

    @Test
    fun `no-match violation exposes unavailable instead of the kernel before-first sentinel`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } returns Eventsequences.AppendResponse.newBuilder()
            .setConcurrencyViolation(
                Eventsequences.ConcurrencyViolation.newBuilder()
                    .setEventSourceId("source")
                    .setExpectedSequenceNumber(EventSequenceNumber.beforeFirst.value)
                    .setActualSequenceNumber(0)
            )
            .build()

        val result = sequenceFor(stub).append("source", ConcurrencyContractEvent("one"))

        assertEquals(EventSequenceNumber.unavailable, result.concurrencyViolations.single().expectedSequenceNumber)
    }

    @Test
    fun `append result reports whether concurrency was checked`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } returns Eventsequences.AppendResponse.newBuilder()
            .setSequenceNumber(4)
            .setConcurrencyCheckPerformed(true)
            .build()

        val result = sequenceFor(stub).append("source", ConcurrencyContractEvent("one"))

        assertTrue(result.concurrencyCheckPerformed)
    }

    @Test
    fun `append many retains every concurrency violation`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(any(), any()) } returns Eventsequences.AppendManyResponse.newBuilder()
            .addConcurrencyViolations(violation("a", 1, 2))
            .addConcurrencyViolations(violation("b", 3, 4))
            .setConcurrencyCheckPerformed(true)
            .build()

        val results = sequenceFor(stub).appendMany(
            listOf(EventForEventSourceId("a", ConcurrencyContractEvent("one"))),
            OperationContext.system()
        )

        assertEquals(listOf("a", "b"), results.single().concurrencyViolations.map { it.eventSourceId })
        assertTrue(results.single().concurrencyCheckPerformed)
    }

    @Test
    fun `get from sequence number sends tag filters`() = runBlocking {
        val request = slot<Eventsequences.GetFromEventSequenceNumberRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.getEventsFromEventSequenceNumber(capture(request), any()) } returns
            Eventsequences.GetFromEventSequenceNumberResponse.getDefaultInstance()

        sequenceFor(stub).getFromSequenceNumber(EventSequenceNumber.first, tags = listOf("blue", "priority"))

        assertEquals(listOf("blue", "priority"), request.captured.tagsList)
    }

    private fun violation(source: String, expected: Long, actual: Long): Eventsequences.ConcurrencyViolation =
        Eventsequences.ConcurrencyViolation.newBuilder()
            .setEventSourceId(source)
            .setExpectedSequenceNumber(expected)
            .setActualSequenceNumber(actual)
            .build()
}
