// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import bcl.Bcl
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.identity.Identity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class EventContextConversionsTests {

    private val correlationId: UUID = UUID.fromString("6f9619ff-8b86-d011-b42d-00cf4fc964ff")

    /** Mirrors how the client writes a UUID onto the wire, so the read path can be checked against it. */
    private fun UUID.toContractsGuid(): Bcl.Guid = Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
        .build()

    private fun reactorContext(): ObservationReactors.EventContext =
        ObservationReactors.EventContext.newBuilder()
            .setEventType(
                ObservationReactors.EventType.newBuilder().setId("BookReturned").setGeneration(2).build()
            )
            .setEventSourceId("book-1")
            .setSequenceNumber(42L)
            .setOccurred(
                ObservationReactors.SerializableDateTimeOffset.newBuilder()
                    .setValue("2026-08-05T10:15:30Z").build()
            )
            .setCorrelationId(correlationId.toContractsGuid())
            .setCausedBy(
                ObservationReactors.Identity.newBuilder()
                    .setSubject("subject-1")
                    .setName("Jane Smith")
                    .setUserName("jane")
                    .build()
            )
            .setEventSourceType("book")
            .setEventStreamType("all")
            .setEventStreamId("default")
            .setEventStore("MyStore")
            .setNamespace("production")
            .setHash("the-hash")
            .addTags("first")
            .addTags("second")
            .setObservationStateValue(EventObservationState.REPLAY or EventObservationState.HEAD_OF_REPLAY)
            .build()

    @Test
    fun `sequence number and event source are carried across`() {
        val context = reactorContext().toEventContext()
        assertEquals(42L, context.sequenceNumber)
        assertEquals("book-1", context.eventSourceId)
    }

    @Test
    fun `event type and generation are carried across`() {
        val context = reactorContext().toEventContext()
        assertEquals("BookReturned", context.eventType.id.value)
        assertEquals(2, context.eventType.generation.value)
    }

    @Test
    fun `occurred is parsed`() {
        assertEquals(Instant.parse("2026-08-05T10:15:30Z"), reactorContext().toEventContext().occurred)
    }

    @Test
    fun `correlation id is read from the wire rather than generated`() {
        assertEquals(correlationId, reactorContext().toEventContext().correlationId)
    }

    @Test
    fun `caused by is read from the wire rather than defaulted to unknown`() {
        val causedBy = reactorContext().toEventContext().causedBy
        assertEquals("subject-1", causedBy.subject)
        assertEquals("Jane Smith", causedBy.name)
        assertEquals("jane", causedBy.userName)
        assertNull(causedBy.onBehalfOf)
    }

    @Test
    fun `caused by falls back to unknown when the kernel sent none`() {
        val context = ObservationReactors.EventContext.newBuilder().setEventSourceId("book-1").build()
        assertEquals(Identity.unknown, context.toEventContext().causedBy)
    }

    @Test
    fun `correlation id is all zeros when the kernel sent none`() {
        val context = ObservationReactors.EventContext.newBuilder().setEventSourceId("book-1").build()
        assertEquals(UUID(0L, 0L), context.toEventContext().correlationId)
    }

    @Test
    fun `on behalf of chain is carried across`() {
        val context = ObservationReactors.EventContext.newBuilder()
            .setCausedBy(
                ObservationReactors.Identity.newBuilder()
                    .setSubject("service")
                    .setName("Service")
                    .setOnBehalfOf(
                        ObservationReactors.Identity.newBuilder().setSubject("user").setName("User").build()
                    )
                    .build()
            )
            .build()
        assertEquals("user", context.toEventContext().causedBy.onBehalfOf?.subject)
    }

    @Test
    fun `stream and store metadata are carried across`() {
        val context = reactorContext().toEventContext()
        assertEquals("book", context.eventSourceType)
        assertEquals("all", context.eventStreamType)
        assertEquals("default", context.eventStreamId)
        assertEquals("MyStore", context.eventStore)
        assertEquals("production", context.namespace)
        assertEquals("the-hash", context.hash)
    }

    @Test
    fun `tags are carried across`() {
        assertEquals(listOf("first", "second"), reactorContext().toEventContext().tags)
    }

    @Test
    fun `observation state flags are carried across`() {
        val state = reactorContext().toEventContext().observationState
        assertTrue(state.isReplay)
        assertTrue(state.isHeadOfReplay)
    }

    @Test
    fun `causation is carried across`() {
        val context = ObservationReactors.EventContext.newBuilder()
            .addCausation(
                ObservationReactors.Causation.newBuilder()
                    .setType("KotlinClient.Append")
                    .setOccurred(
                        ObservationReactors.SerializableDateTimeOffset.newBuilder()
                            .setValue("2026-08-05T10:15:30Z").build()
                    )
                    .putProperties("key", "value")
                    .build()
            )
            .build()
            .toEventContext()

        assertEquals(1, context.causation.size)
        assertEquals("KotlinClient.Append", context.causation[0].type.name)
        assertEquals("value", context.causation[0].properties["key"])
    }

    @Test
    fun `unparseable occurred does not fail the conversion`() {
        val context = ObservationReactors.EventContext.newBuilder()
            .setOccurred(
                ObservationReactors.SerializableDateTimeOffset.newBuilder().setValue("not-a-date").build()
            )
            .build()
        assertTrue(context.toEventContext().occurred.isAfter(Instant.EPOCH))
    }

    @Test
    fun `reducer context is converted the same way`() {
        val context = ObservationReducers.EventContext.newBuilder()
            .setEventType(
                ObservationReducers.EventType.newBuilder().setId("BookBorrowed").setGeneration(1).build()
            )
            .setEventSourceId("book-2")
            .setSequenceNumber(7L)
            .setCorrelationId(correlationId.toContractsGuid())
            .setCausedBy(
                ObservationReducers.Identity.newBuilder().setSubject("subject-1").setName("Jane").build()
            )
            .setObservationStateValue(EventObservationState.INITIAL)
            .build()
            .toEventContext()

        assertEquals("BookBorrowed", context.eventType.id.value)
        assertEquals("book-2", context.eventSourceId)
        assertEquals(7L, context.sequenceNumber)
        assertEquals(correlationId, context.correlationId)
        assertEquals("subject-1", context.causedBy.subject)
        assertTrue(context.observationState.isInitial)
    }
}
