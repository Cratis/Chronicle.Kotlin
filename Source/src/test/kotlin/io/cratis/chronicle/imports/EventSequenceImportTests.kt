// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.toUuid
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@EventType(id = "legacy-customer-registered", generation = 3)
data class LegacyCustomerRegistered(val name: String)

/**
 * An event sequence imports through the kernel's `Imports` service: it begins the session, carries what
 * each event says about itself onto the wire, and reattaches to a session by its identifier.
 */
class EventSequenceImportTests {
    private val calls = FakeImportCalls(maxChunkEvents = 100)
    private val sequence = EventSequence(
        EventSequenceId.eventLog,
        "store",
        "default",
        mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>(),
    ).also { it.importCalls = calls }

    @Test
    fun `an import begins on the sequence, sends every event and completes`() = runBlocking {
        val result = sequence.import((1..3).map { EventForEventSourceId("customer-$it", LegacyCustomerRegistered("c$it")) }.asFlow())

        assertTrue(result.isSuccess)
        val begun = calls.begun.single()
        assertEquals("event-log", begun.eventSequenceId)
        assertEquals("store", begun.eventStore)
        assertEquals(Contracts.OnInvalidEvent.Reject, begun.onInvalid)
        assertEquals(listOf("customer-1", "customer-2", "customer-3"), calls.chunks.flatMap { c -> c.eventsList.map { it.eventSourceId } })
        assertEquals(3L, calls.completed.single().expectedEvents)
    }

    @Test
    fun `a lazy sequence is imported too`() = runBlocking {
        val result = sequence.import(generateSequence(1) { it + 1 }.take(5).map { EventForEventSourceId("c$it", LegacyCustomerRegistered("x")) })

        assertTrue(result.isSuccess)
        assertEquals(5, calls.chunks.sumOf { it.eventsCount })
    }

    @Test
    fun `an imported event carries what it says about itself`() = runBlocking {
        val correlationId = UUID.randomUUID()
        val occurred = Instant.parse("2019-03-04T05:06:07Z")
        val causedBy = Identity("subject-1", "Ada", "ada")
        val causation = Causation(occurred, CausationType("Legacy"), mapOf("system" to "erp"))

        sequence.import(
            listOf(
                EventForEventSourceId(
                    eventSourceId = "customer-1",
                    event = LegacyCustomerRegistered("Ada"),
                    eventStreamType = "Customers",
                    eventStreamId = "Nordic",
                    eventSourceType = "Customer",
                    tags = listOf("migrated"),
                    occurred = occurred,
                    subject = "person-1",
                    causation = listOf(causation),
                    correlationId = correlationId,
                    causedBy = causedBy,
                    generation = 1,
                    sourceSequenceNumber = 42,
                    idempotencyKey = "erp-42",
                ),
            ).asFlow(),
        )

        val wire = calls.chunks.single().eventsList.single()
        assertEquals("customer-1", wire.eventSourceId)
        assertEquals("Customer", wire.eventSourceType)
        assertEquals("Customers", wire.eventStreamType)
        assertEquals("Nordic", wire.eventStreamId)
        assertEquals("legacy-customer-registered", wire.eventType.id)
        assertEquals(1, wire.eventType.generation)
        assertTrue(wire.content.contains("Ada"))
        assertEquals(listOf("migrated"), wire.tagsList)
        assertEquals("2019-03-04T05:06:07Z", wire.occurred.value)
        assertEquals("person-1", wire.subject)
        assertEquals(correlationId, wire.correlationId.toUuid())
        assertEquals("Legacy", wire.causationList.single().type)
        assertEquals(mapOf("system" to "erp"), wire.causationList.single().propertiesMap)
        assertEquals("Ada", wire.causedBy.name)
        assertEquals(42L, wire.sourceSequenceNumber)
        assertEquals("erp-42", wire.idempotencyKey)
    }

    @Test
    fun `an event that says nothing more takes its declared generation and its event source as subject`() = runBlocking {
        sequence.import(listOf(EventForEventSourceId("customer-1", LegacyCustomerRegistered("Ada"))).asFlow())

        val wire = calls.chunks.single().eventsList.single()
        assertEquals(3, wire.eventType.generation)
        assertEquals("customer-1", wire.subject)
        assertTrue(!wire.hasCorrelationId() && !wire.hasCausedBy() && !wire.hasOccurred())
        assertEquals("", wire.idempotencyKey)
    }

    @Test
    fun `progress is handed to the caller while the import runs`() = runBlocking {
        calls.progressUpdates.emit(calls.progress(Contracts.ImportState.Staging) { setEventsReceived(1) })
        val reported = CopyOnWriteArrayList<ImportProgress>()
        calls.onChunk = { request ->
            // Gives the progress collector a chance to run before the import ends.
            kotlinx.coroutines.withTimeout(5_000) { while (reported.isEmpty()) kotlinx.coroutines.yield() }
            FakeImportCalls.accepted(request)
        }

        sequence.import(listOf(EventForEventSourceId("c", LegacyCustomerRegistered("x"))).asFlow(), onProgress = FlowCollector { reported.add(it) })

        assertEquals(ImportState.Staging, reported.first().state)
        assertEquals(1L, reported.first().eventsReceived)
    }

    @Test
    fun `an import can begin under an identifier of its own`() = runBlocking {
        val importId = ImportId.create()

        val import = sequence.beginImport(ImportOptions(importId = importId, onInvalid = OnInvalidEvent.Skip))

        assertEquals(importId, import.importId)
        assertEquals(importId.value, calls.begun.single().importId.toUuid())
        assertEquals(Contracts.OnInvalidEvent.Skip, calls.begun.single().onInvalid)
    }

    @Test
    fun `an import the kernel refuses to begin is reported`() {
        calls.beginError = "The event sequence is read-only"

        val error = assertThrows<ImportCouldNotBegin> { runBlocking { sequence.beginImport() } }

        assertEquals("The event sequence is read-only", error.message)
    }

    @Test
    fun `reattaching continues after what the kernel acknowledged`() = runBlocking {
        calls.reads = {
            calls.progress(Contracts.ImportState.Staging) {
                setOnInvalid(Contracts.OnInvalidEvent.Skip)
                setMaxChunkEvents(2)
                setAcknowledgedChunks(2)
                setAcknowledgedEvents(4)
            }
        }

        val import = sequence.beginImport(ImportId(calls.importId))
        import.add(listOf(EventForEventSourceId("c5", LegacyCustomerRegistered("x"))))
        calls.reads = { calls.progress(Contracts.ImportState.Completed) }
        import.complete()

        assertEquals(4L, import.eventsAcknowledged)
        assertEquals(2, calls.chunks.single().chunkIndex)
        assertEquals(4L, calls.chunks.single().firstEventIndex)
        assertTrue(calls.begun.isEmpty())
    }

    @Test
    fun `reattaching to an import that does not exist is refused`() {
        calls.reads = { null }

        assertThrows<ImportCouldNotBegin> { runBlocking { sequence.beginImport(ImportId.create()) } }
    }

    @Test
    fun `reattaching to an import into another sequence is refused`() {
        calls.reads = { calls.progress(Contracts.ImportState.Staging).toBuilder().setEventSequenceId("outbox").build() }

        val error = assertThrows<ImportCouldNotBegin> { runBlocking { sequence.beginImport(ImportId(calls.importId)) } }

        assertTrue(error.message!!.contains("outbox"))
    }

    @Test
    fun `a sequence without the imports service says so`() {
        val unsupported = EventSequence(EventSequenceId.eventLog, "store", "default", mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>())

        assertThrows<ImportsNotSupported> { runBlocking { unsupported.beginImport() } }
    }
}
