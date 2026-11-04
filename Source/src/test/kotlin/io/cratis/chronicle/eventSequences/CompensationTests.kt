// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.events.CompensationFor
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType("entry-posted")
private data class EntryPosted(val amount: Long)

@EventType
private data class UnrelatedHappened(val value: String)

@EventType("entry-reversed")
@CompensationFor(EntryPosted::class)
private data class EntryReversed(val reason: String)

@CompensationFor(UnrelatedHappened::class)
private data class NotAnEventTypeCompensation(val reason: String)

@EventType
@CompensationFor(UnannotatedTarget::class)
private data class CompensatesUnannotated(val reason: String)

private class UnannotatedTarget


class CompensationTests {
    private fun appendResponse(sequenceNumber: Long, vararg errors: String) =
        Sequences.CommandResult_AppendResponse.newBuilder()
            .setIsAuthorized(true)
            .setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(sequenceNumber).addAllErrors(errors.toList()))
            .build()

    @Test
    fun `compensate sends the compensated sequence number, event type and content on the wire`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.CompensateRequest>()
        coEvery { stub.compensate(capture(request), any()) } returns appendResponse(8)
        val correlationId = UUID.randomUUID()
        val occurred = Instant.parse("2026-01-02T03:04:05Z")

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val result = sequence.compensate(
            EventSequenceNumber(7),
            EntryReversed("wrong account"),
            CompensateOptions(correlationId = correlationId, tags = listOf("ledger"), occurred = occurred)
        )

        assertTrue(result.isSuccess)
        assertEquals(8L, result.sequenceNumberValue)
        assertEquals(7L, request.captured.compensationFor)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
        assertEquals(EventSequenceId.eventLog.value, request.captured.eventSequenceId)
        assertEquals("entry-reversed", request.captured.eventType.id)
        assertEquals(1, request.captured.eventType.generation)
        assertTrue(request.captured.content.contains("wrong account"))
        assertEquals(listOf("ledger"), request.captured.tagsList)
        assertEquals(occurred.toString(), request.captured.occurred.value)
    }

    @Test
    fun `compensate reports a rejection as an append error rather than throwing`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.compensate(any(), any()) } returns appendResponse(0, AppendError.COMPENSATED_EVENT_NOT_FOUND)

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val result = sequence.compensate(EventSequenceNumber(99), EntryReversed("nope"))

        assertFalse(result.isSuccess)
        assertTrue(result.errors.single().isCompensatedEventNotFound)
        assertFalse(result.errors.single().isEventTypeDoesNotCompensateEventType)
    }

    @Test
    fun `compensate emits on appendOperations with the link back to the original`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.compensate(any(), any()) } returns appendResponse(8)
        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val emitted = mutableListOf<AppendedEventWithResult>()
        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            sequence.appendOperations.collect { emitted.addAll(it) }
        }

        sequence.compensate(EventSequenceNumber(7), EntryReversed("wrong account"))
        yield()
        job.cancel()

        assertEquals(1, emitted.size)
        assertEquals(7L, emitted.single().context.compensationFor)
        assertEquals(8L, emitted.single().context.sequenceNumber)
    }

    @Test
    fun `an event sequence that cannot compensate says so`() {
        assertThrows(CompensationsNotSupported::class.java) {
            runBlocking { IEventSequenceWithoutCompensation().compensate(EventSequenceNumber(1), EntryReversed("x")) }
        }
    }

    @Test
    fun `the schema of a compensation event type names the event type it compensates for`() {
        val schema = com.google.gson.JsonParser.parseString(JsonSchemaGenerator.generate(EntryReversed::class)).asJsonObject

        assertEquals("entry-posted", schema.get(JsonSchemaGenerator.COMPENSATION_FOR_KEY).asString)
    }

    @Test
    fun `the compensated event type falls back to its class name when its id is not declared`() {
        val schema = com.google.gson.JsonParser.parseString(JsonSchemaGenerator.generate(NotAnEventTypeCompensation::class)).asJsonObject

        assertEquals("UnrelatedHappened", schema.get(JsonSchemaGenerator.COMPENSATION_FOR_KEY).asString)
    }

    @Test
    fun `the schema of an ordinary event type has no compensation entry`() {
        val schema = com.google.gson.JsonParser.parseString(JsonSchemaGenerator.generate(EntryPosted::class)).asJsonObject

        assertFalse(schema.has(JsonSchemaGenerator.COMPENSATION_FOR_KEY))
    }

    @Test
    fun `compensating something that is not an event type is rejected when the schema is built`() {
        assertThrows(IllegalArgumentException::class.java) { JsonSchemaGenerator.generate(CompensatesUnannotated::class) }
    }

    @Test
    fun `an event context reads the wire value zero as not a compensation`() {
        val ordinary = Sequences.EventContext.newBuilder().build()
        val context = EventContext(
            sequenceNumber = 1,
            eventSourceId = "s",
            eventType = io.cratis.chronicle.events.EventTypeDescriptor(
                io.cratis.chronicle.events.EventTypeId("e"),
                io.cratis.chronicle.events.EventTypeGeneration(1)
            ),
            occurred = Instant.now(),
            correlationId = UUID.randomUUID(),
            causedBy = io.cratis.chronicle.identity.Identity.unknown
        )

        assertEquals(0L, ordinary.compensationFor)
        assertNull(context.compensationFor)
        assertFalse(context.isCompensation)
        assertEquals(7L, context.copy(compensationFor = 7L).compensationFor)
        assertTrue(context.copy(compensationFor = 7L).isCompensation)
    }

    @Test
    fun `copy through the earlier shapes keeps the compensated sequence number`() {
        val context = EventContext(
            sequenceNumber = 1,
            eventSourceId = "s",
            eventType = io.cratis.chronicle.events.EventTypeDescriptor(
                io.cratis.chronicle.events.EventTypeId("e"),
                io.cratis.chronicle.events.EventTypeGeneration(1)
            ),
            occurred = Instant.now(),
            correlationId = UUID.randomUUID(),
            causedBy = io.cratis.chronicle.identity.Identity.unknown,
            compensationFor = 4L
        )

        assertEquals(4L, context.copy(subject = "changed").compensationFor)
        assertEquals(4L, context.copy(sequenceNumber = 2, eventSource = "x").compensationFor)
    }
}

/** An implementation that, like an older one, defines nothing about compensating. */
private class IEventSequenceWithoutCompensation : IEventSequence by mockk(relaxed = true) {
    override suspend fun compensate(compensationFor: EventSequenceNumber, event: Any, options: CompensateOptions?): AppendResult =
        super<IEventSequence>.compensate(compensationFor, event, options)
}
