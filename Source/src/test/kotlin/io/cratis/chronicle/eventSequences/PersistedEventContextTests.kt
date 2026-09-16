// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import bcl.Bcl
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.identity.Identity
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PersistedEventContextTests {
    private val occurred = Instant.parse("2001-02-03T04:05:06Z")
    private val correlationId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `both persisted reads return stored routing and metadata without recomputing defaults`(readBySource: Boolean) = runBlocking {
        val stored = wireEvent("Account", "Default", "source-1")
        val events = read(readBySource, stored)

        assertEquals(1, events.size)
        assertEquals("{\"name\":\"stored\"}", events.single().content)
        val context = events.single().context
        assertEquals(17L, context.sequenceNumber)
        assertEquals("source-1", context.eventSourceId)
        assertEquals("PersistedEvent", context.eventType.id.value)
        assertEquals(3, context.eventType.generation.value)
        assertTrue(context.eventType.tombstone)
        assertEquals("Account", context.eventSourceType)
        assertEquals("Default", context.eventStreamType)
        assertEquals("source-1", context.eventStreamId)
        assertEquals("queried-store", context.eventStore)
        assertEquals("queried-tenant", context.namespace)
        assertEquals(occurred, context.occurred)
        assertEquals(correlationId, context.correlationId)
        assertEquals(Identity("writer", "Writer", "writer-name", Identity("person", "Person", "person-name")), context.causedBy)
        assertEquals(listOf(Causation(occurred, CausationType("Import"), mapOf("file" to "old.csv"))), context.causation)
        assertEquals(listOf("a", "b"), context.tags)
        assertEquals("stored-hash", context.hash)
        assertEquals(EventObservationState(1), context.observationState)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `stored empty routing stays empty rather than becoming client defaults`(readBySource: Boolean) = runBlocking {
        val events = read(readBySource, wireEvent("", "", ""))
        assertEquals(1, events.size)
        assertEquals("", events.single().context.eventSourceType)
        assertEquals("", events.single().context.eventStreamType)
        assertEquals("", events.single().context.eventStreamId)
    }

    private suspend fun read(readBySource: Boolean, stored: Sequences.AppendedEventResponse): List<AppendedEvent> {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val response = Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder()
            .setIsAuthorized(true).addData(stored).build()
        coEvery { stub.forEventSourceIdAndEventTypes(any(), any()) } returns response
        coEvery { stub.fromSequenceNumber(any(), any()) } returns response
        val sequence = EventSequence(EventSequenceId.eventLog, "queried-store", "queried-tenant", stub)
        return if (readBySource) sequence.getForEventSourceIdAndEventTypes("source-1", emptyList())
            else sequence.getFromSequenceNumber(EventSequenceNumber(17))
    }

    private fun wireEvent(sourceType: String, streamType: String, streamId: String): Sequences.AppendedEventResponse =
        Sequences.AppendedEventResponse.newBuilder()
            .setContent("{\"name\":\"stored\"}")
            .setContext(Sequences.EventContext.newBuilder()
                .setSequenceNumber(17).setEventSourceId("source-1")
                .setEventSourceType(sourceType).setEventStreamType(streamType).setEventStreamId(streamId)
                .setEventType(Sequences.EventType.newBuilder().setId("PersistedEvent").setGeneration(3).setTombstone(true))
                .setOccurred(Sequences.SerializableDateTimeOffset.newBuilder().setValue(occurred.toString()))
                .setCorrelationId(Bcl.Guid.newBuilder()
                    .setLo(java.lang.Long.reverseBytes(correlationId.mostSignificantBits))
                    .setHi(java.lang.Long.reverseBytes(correlationId.leastSignificantBits)))
                .setCausedBy(Sequences.Identity.newBuilder().setSubject("writer").setName("Writer").setUserName("writer-name")
                    .setOnBehalfOf(Sequences.Identity.newBuilder().setSubject("person").setName("Person").setUserName("person-name")))
                .addCausation(Sequences.Causation.newBuilder().setType("Import")
                    .setOccurred(Sequences.SerializableDateTimeOffset.newBuilder().setValue(occurred.toString()))
                    .putProperties("file", "old.csv"))
                .addAllTags(listOf("a", "b")).setHash("stored-hash").setObservationStateValue(1))
            .build()
}
