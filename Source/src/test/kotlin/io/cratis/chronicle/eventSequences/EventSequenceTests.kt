// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.Sequences
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import bcl.Bcl
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun UUID.toContractGuid(): Bcl.Guid = Bcl.Guid.newBuilder()
    .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
    .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
    .build()

private fun sampleEventContext(sequenceNumber: Long): Sequences.EventContext =
    Sequences.EventContext.newBuilder()
        .setSequenceNumber(sequenceNumber)
        .setEventSourceId("source-1")
        .setEventType(Sequences.EventType.newBuilder().setId("ObservedEvent").setGeneration(1))
        .setOccurred(Sequences.SerializableDateTimeOffset.newBuilder().setValue(java.time.Instant.now().toString()))
        .setCorrelationId(UUID.randomUUID().toContractGuid())
        .setCausedBy(
            Sequences.Identity.newBuilder()
                .setSubject("system")
                .setName("System")
                .setUserName("system")
        )
        .build()

private data class SomethingHappened(val value: String)

@EventType
private data class ObservedEvent(val value: String)

private class ObserverWithHandler {
    @Suppress("UNUSED_PARAMETER")
    fun handle(event: ObservedEvent) {}
}

class EventSequenceTests {

    @Test
    fun `append sends the supplied concurrency scope on the wire instead of a hardcoded disabled scope`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.AppendRequest>()
        coEvery { stub.append(capture(request), any()) } returns
            Sequences.CommandResult_AppendResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(0).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val scope = ConcurrencyScope(EventSequenceNumber(7), eventSourceId = true, eventStreamType = "Onboarding")

        sequence.append("source-1", SomethingHappened("hello"), AppendOptions(concurrencyScope = scope))

        val sentScope = request.captured.concurrencyScope
        assertEquals(7L, sentScope.sequenceNumber)
        assertTrue(sentScope.eventSourceId)
        assertEquals("Onboarding", sentScope.eventStreamType)
    }

    @Test
    fun `append with no explicit concurrency scope disables concurrency validation`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.AppendRequest>()
        coEvery { stub.append(capture(request), any()) } returns
            Sequences.CommandResult_AppendResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(0).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        sequence.append("source-1", SomethingHappened("hello"))

        assertEquals(EventSequenceNumber.unavailable.value, request.captured.concurrencyScope.sequenceNumber)
    }

    @Test
    fun `append surfaces a concurrency violation returned by the kernel as a failed result`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } returns
            Sequences.CommandResult_AppendResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    Sequences.AppendResponse.newBuilder()
                        .setConcurrencyViolation(
                            Sequences.ConcurrencyViolation.newBuilder()
                                .setEventSourceId("source-1")
                                .setExpectedSequenceNumber(3)
                                .setActualSequenceNumber(5)
                                .build()
                        )
                        .build()
                )
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val scope = ConcurrencyScope(EventSequenceNumber(3), eventSourceId = true)

        val result = sequence.append("source-1", SomethingHappened("hello"), AppendOptions(concurrencyScope = scope))

        assertFalse(result.isSuccess)
        assertNotNull(result.concurrencyViolation)
        assertEquals("source-1", result.concurrencyViolation?.eventSourceId)
        assertEquals(EventSequenceNumber(3), result.concurrencyViolation?.expectedSequenceNumber)
        assertEquals(EventSequenceNumber(5), result.concurrencyViolation?.actualSequenceNumber)
    }

    @Test
    fun `appendMany commits every event through a single atomic AppendMany call`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val manyRequest = slot<Sequences.AppendManyRequest>()
        coEvery { stub.appendMany(capture(manyRequest), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers(listOf(0L, 1L, 2L)).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val events = listOf(SomethingHappened("a"), SomethingHappened("b"), SomethingHappened("c"))

        val results = sequence.appendMany("source-1", events)

        // Exactly one RPC call for the whole batch - never one Append per event.
        io.mockk.coVerify(exactly = 1) { stub.appendMany(any(), any()) }
        io.mockk.coVerify(exactly = 0) { stub.append(any(), any()) }

        assertEquals(3, manyRequest.captured.eventsList.size)
        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(listOf(0L, 1L, 2L), results.map { it.sequenceNumber.value })
    }

    @Test
    fun `appendMany sends the concurrency scope for the event source it targets`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val manyRequest = slot<Sequences.AppendManyRequest>()
        coEvery { stub.appendMany(capture(manyRequest), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers(listOf(0L, 1L)).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val scope = ConcurrencyScope(EventSequenceNumber(10), eventSourceId = true)

        sequence.appendMany(
            "source-1",
            listOf(SomethingHappened("a"), SomethingHappened("b")),
            AppendOptions(concurrencyScope = scope)
        )

        // The single-event-source batch request already targets exactly one event source, so the
        // wire carries one scope for the whole request rather than a scope keyed per source.
        assertEquals("source-1", manyRequest.captured.eventSourceId)
        assertEquals(10L, manyRequest.captured.concurrencyScope.sequenceNumber)
        assertTrue(manyRequest.captured.concurrencyScope.eventSourceId)
    }

    @Test
    fun `appendMany surfaces a batch-level failure on every returned result`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(any(), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addErrors("something went wrong").build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val results = sequence.appendMany("source-1", listOf(SomethingHappened("a"), SomethingHappened("b")))

        assertEquals(2, results.size)
        assertTrue(results.all { !it.isSuccess })
        assertTrue(results.all { it.errors.any { error -> error.message == "something went wrong" } })
    }

    @Test
    fun `appendMany across event sources commits them all through a single atomic call`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val manyRequest = slot<Sequences.AppendManyForEventSourcesRequest>()
        coEvery { stub.appendManyForEventSources(capture(manyRequest), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers(listOf(0L, 1L)).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

        val results = sequence.appendMany(
            listOf(
                EventForEventSourceId("source-1", SomethingHappened("a")),
                EventForEventSourceId("source-2", SomethingHappened("b"), eventStreamType = "Onboarding")
            )
        )

        io.mockk.coVerify(exactly = 1) { stub.appendManyForEventSources(any(), any()) }
        assertEquals(listOf("source-1", "source-2"), manyRequest.captured.eventsList.map { it.eventSourceId })
        // Each event resolves its own defaults against its own event source id.
        assertEquals("Default", manyRequest.captured.eventsList[0].eventStreamType)
        assertEquals("source-1", manyRequest.captured.eventsList[0].eventStreamId)
        assertEquals("Onboarding", manyRequest.captured.eventsList[1].eventStreamType)
        assertEquals("source-2", manyRequest.captured.eventsList[1].subject)
        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `appendMany across event sources sends a concurrency scope per event source`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val manyRequest = slot<Sequences.AppendManyForEventSourcesRequest>()
        coEvery { stub.appendManyForEventSources(capture(manyRequest), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers(listOf(0L, 1L)).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

        sequence.appendMany(
            listOf(
                EventForEventSourceId("source-1", SomethingHappened("a")),
                EventForEventSourceId("source-2", SomethingHappened("b"))
            ),
            concurrencyScopes = mapOf("source-1" to ConcurrencyScope(EventSequenceNumber(5), eventSourceId = true))
        )

        // A source left out of the map is appended unchecked rather than validated against nothing.
        val scopes = manyRequest.captured.concurrencyScopesList
        assertEquals(setOf("source-1"), scopes.map { it.eventSourceId }.toSet())
        assertEquals(5L, scopes.single { it.eventSourceId == "source-1" }.scope.sequenceNumber)
    }

    @Test
    fun `appendMany with an empty event list does not call the kernel at all`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val results = sequence.appendMany("source-1", emptyList())

        assertTrue(results.isEmpty())
        io.mockk.coVerify(exactly = 0) { stub.appendMany(any(), any()) }
    }

    @Test
    fun `getTailSequenceNumber returns the sequence number reported by the kernel`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.TailSequenceNumberRequest>()
        coEvery { stub.tailSequenceNumber(capture(request), any()) } returns
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder()
                .setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(41).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val tail = sequence.getTailSequenceNumber("source-1")

        assertEquals(EventSequenceNumber(41), tail)
        assertEquals("source-1", request.captured.eventSourceId)
    }

    @Test
    fun `getTailSequenceNumber without an event source id queries across all event sources`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.TailSequenceNumberRequest>()
        coEvery { stub.tailSequenceNumber(capture(request), any()) } returns
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder()
                .setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(9).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val tail = sequence.getTailSequenceNumber()

        assertEquals(EventSequenceNumber(9), tail)
        assertEquals("", request.captured.eventSourceId)
    }

    @Test
    fun `getTailSequenceNumberForObserver filters by the event types the observer handles`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.TailSequenceNumberRequest>()
        coEvery { stub.tailSequenceNumber(capture(request), any()) } returns
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder()
                .setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(3).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val tail = sequence.getTailSequenceNumberForObserver(ObserverWithHandler::class)

        assertEquals(EventSequenceNumber(3), tail)
        // The kernel takes filter event types as a single comma-joined id string, not a repeated field.
        assertEquals("ObservedEvent", request.captured.eventTypeIds)
    }

    @Test
    fun `completeStream returns Success with the tail sequence number on success`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.completeStream(any(), any()) } returns
            Sequences.CommandResult_CompleteStreamResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    Sequences.CompleteStreamResponse.newBuilder()
                        .setIsSuccess(true)
                        .setSequenceNumber(9)
                        .build()
                )
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val result = sequence.completeStream("Onboarding", "2026")

        assertEquals(CompleteStreamResult.Success(EventSequenceNumber(9)), result)
    }

    @Test
    fun `completeStream returns DefaultStreamCannotBeCompleted when the kernel rejects the default stream`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.completeStream(any(), any()) } returns
            Sequences.CommandResult_CompleteStreamResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    Sequences.CompleteStreamResponse.newBuilder()
                        .setIsSuccess(false)
                        .setError(Sequences.CompleteStreamError.DefaultStreamCannotBeCompleted)
                        .build()
                )
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val result = sequence.completeStream("Default", "")

        assertEquals(CompleteStreamResult.DefaultStreamCannotBeCompleted, result)
    }

    @Test
    fun `completeStream returns AlreadyCompleted when the stream was already completed`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.completeStream(any(), any()) } returns
            Sequences.CommandResult_CompleteStreamResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    Sequences.CompleteStreamResponse.newBuilder()
                        .setIsSuccess(false)
                        .setError(Sequences.CompleteStreamError.AlreadyCompleted)
                        .build()
                )
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val result = sequence.completeStream("Onboarding", "2026")

        assertEquals(CompleteStreamResult.AlreadyCompleted, result)
    }

    @Test
    fun `redact sends the sequence number and reason on the wire`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.RedactRequest>()
        coEvery { stub.redact(capture(request), any()) } returns
            Sequences.CommandResult.newBuilder().setIsAuthorized(true).build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        sequence.redact(EventSequenceNumber(12), RedactionReason("GDPR erasure request"))

        assertEquals(12L, request.captured.sequenceNumber)
        assertEquals("GDPR erasure request", request.captured.reason)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals(EventSequenceId.eventLog.value, request.captured.eventSequenceId)
    }

    @Test
    fun `redactForEventSource sends the event source id, reason, and event types on the wire`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.RedactForEventSourceRequest>()
        coEvery { stub.redactForEventSource(capture(request), any()) } returns
            Sequences.CommandResult.newBuilder().setIsAuthorized(true).build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        sequence.redactForEventSource("source-1", RedactionReason("GDPR erasure request"), listOf(ObservedEvent::class))

        assertEquals("source-1", request.captured.eventSourceId)
        assertEquals("GDPR erasure request", request.captured.reason)
        // EventTypes travels as a repeated string of event type ids, not a repeated EventType message.
        assertEquals(1, request.captured.eventTypesList.size)
        assertEquals("ObservedEvent", request.captured.eventTypesList.single())
    }

    @Test
    fun `redactForEventSource with no event types redacts every event type for the event source`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.RedactForEventSourceRequest>()
        coEvery { stub.redactForEventSource(capture(request), any()) } returns
            Sequences.CommandResult.newBuilder().setIsAuthorized(true).build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        sequence.redactForEventSource("source-1", RedactionReason.unknown)

        assertTrue(request.captured.eventTypesList.isEmpty())
        assertEquals("Unknown", request.captured.reason)
    }

    @Test
    fun `appendOperations emits after a successful append through this instance`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } returns
            Sequences.CommandResult_AppendResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(4).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val received = CompletableDeferred<List<AppendedEventWithResult>>()
        val job = launch { sequence.appendOperations.collect { received.complete(it) } }
        yield() // let the collector subscribe before the append happens

        sequence.append("source-1", SomethingHappened("hello"))

        val emission = withTimeout(2000) { received.await() }
        job.cancel()

        assertEquals(1, emission.size)
        val entry = emission.single()
        assertEquals("source-1", entry.context.eventSourceId)
        assertTrue(entry.result.isSuccess)
        assertEquals(EventSequenceNumber(4), entry.result.sequenceNumber)
        assertEquals(SomethingHappened("hello"), entry.event)
    }

    @Test
    fun `appendOperations emits the full batch as a single entry for appendMany`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(any(), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers(listOf(0L, 1L)).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val received = CompletableDeferred<List<AppendedEventWithResult>>()
        val job = launch { sequence.appendOperations.collect { received.complete(it) } }
        yield()

        sequence.appendMany("source-1", listOf(SomethingHappened("a"), SomethingHappened("b")))

        val emission = withTimeout(2000) { received.await() }
        job.cancel()

        assertEquals(2, emission.size)
        assertTrue(emission.all { it.result.isSuccess })
    }

    @Test
    fun `getForEventSourceIdAndEventTypes sends the event source id and event types on the wire`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.ForEventSourceIdAndEventTypesRequest>()
        val appendedEvent = Sequences.AppendedEventResponse.newBuilder()
            .setContext(sampleEventContext(3))
            .setContent("""{"value":"hello"}""")
            .build()
        coEvery {
            stub.forEventSourceIdAndEventTypes(capture(request), any())
        } returns Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder()
            .setIsAuthorized(true)
            .addData(appendedEvent)
            .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val events = sequence.getForEventSourceIdAndEventTypes("source-1", listOf(ObservedEvent::class))

        assertEquals("source-1", request.captured.eventSourceId)
        assertEquals("ObservedEvent", request.captured.eventTypeIds)
        assertEquals(1, events.size)
        assertEquals("""{"value":"hello"}""", events.single().content)
        assertEquals(3L, events.single().context.sequenceNumber)
    }

    @Test
    fun `getForEventSourceIdAndEventTypes narrows by stream type and stream id when supplied`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.ForEventSourceIdAndEventTypesRequest>()
        coEvery {
            stub.forEventSourceIdAndEventTypes(capture(request), any())
        } returns Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder()
            .setIsAuthorized(true)
            .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        // eventSourceType has nowhere to go on the wire in v18 - ForEventSourceIdAndEventTypesRequest
        // carries no such field - but the parameter stays accepted for API compatibility.
        sequence.getForEventSourceIdAndEventTypes(
            "source-1",
            listOf(ObservedEvent::class),
            eventStreamType = "Onboarding",
            eventStreamId = "2026",
            eventSourceType = "Account"
        )

        assertEquals("Onboarding", request.captured.eventStreamType)
        assertEquals("2026", request.captured.eventStreamId)
    }

    @Test
    fun `getFromSequenceNumber sends the starting sequence number and returns the mapped events`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.FromSequenceNumberRequest>()
        val appendedEvent = Sequences.AppendedEventResponse.newBuilder()
            .setContext(sampleEventContext(10))
            .setContent("""{"value":"world"}""")
            .build()
        coEvery {
            stub.fromSequenceNumber(capture(request), any())
        } returns Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder()
            .setIsAuthorized(true)
            .addData(appendedEvent)
            .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val events = sequence.getFromSequenceNumber(EventSequenceNumber(10), eventSourceId = "source-1")

        assertEquals(10L, request.captured.fromEventSequenceNumber)
        assertEquals("source-1", request.captured.eventSourceId)
        assertEquals(1, events.size)
        assertEquals("""{"value":"world"}""", events.single().content)
    }

    @Test
    fun `getFromSequenceNumber omits event source id and event types when not supplied`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Sequences.FromSequenceNumberRequest>()
        coEvery {
            stub.fromSequenceNumber(capture(request), any())
        } returns Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder()
            .setIsAuthorized(true)
            .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        sequence.getFromSequenceNumber(EventSequenceNumber(0))

        assertEquals("", request.captured.eventSourceId)
        assertEquals("", request.captured.eventTypeIds)
    }

    @Test
    fun `getNextSequenceNumber returns the first sequence number when the sequence is empty`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.tailSequenceNumber(any(), any()) } returns
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder()
                .setIsAuthorized(true)
                .setData(
                    Sequences.EventSequenceTailResponse.newBuilder()
                        .setSequenceNumber(EventSequenceNumber.unavailable.value)
                        .build()
                )
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val next = sequence.getNextSequenceNumber()

        assertEquals(EventSequenceNumber.first, next)
    }

    @Test
    fun `getNextSequenceNumber returns one past the current tail`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.tailSequenceNumber(any(), any()) } returns
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder()
                .setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(6).build())
                .build()

        val sequence = EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)
        val next = sequence.getNextSequenceNumber()

        assertEquals(EventSequenceNumber(7), next)
    }
}
