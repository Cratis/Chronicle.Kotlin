// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import bcl.Bcl
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.lang.Long.reverseBytes
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@EventType(generation = 2)
private data class BatchEvent(val value: String)

class RoutedBatchMetadataTests {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `routing a single source batch retains all append metadata`(java: Boolean) = runBlocking {
        val request = slot<Sequences.AppendManyForEventSourcesRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendManyForEventSources(capture(request), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addSequenceNumbers(4).addSequenceNumbers(5)).build()
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)
        val correlationId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
        val occurred = Instant.parse("2000-02-03T04:05:06Z")
        val causation = listOf(Causation(occurred, CausationType("Import"), mapOf("file" to "events.csv")))
        val identity = Identity("operator", "Operator", "operator-name", Identity("importer", "Importer", "import"))
        val previousIdentity = identityProvider.currentIdentity
        val previousCausation = causationManager.currentChain
        identityProvider.setCurrentIdentity(identity)
        try {
            for (subject in listOf(null, "", "person-42")) {
                val options = AppendOptions(correlationId = correlationId,
                    eventSourceType = "Account", eventStreamType = "Orders", eventStreamId = "2026",
                    subject = subject, occurred = occurred, tags = listOf("import", "history"), causation = causation)
                val events = listOf(BatchEvent("a"), BatchEvent("b"))
                if (java) JavaAppendRoutingUsage.appendMany(sequence, events, options)
                else sequence.appendMany("source-1", events, options)

                val sent = request.captured
                assertEquals("store", sent.eventStore)
                assertEquals("tenant", sent.namespace)
                assertEquals(EventSequenceId.eventLog.value, sent.eventSequenceId)
                assertEquals(Bcl.Guid.newBuilder()
                    .setLo(reverseBytes(correlationId.mostSignificantBits))
                    .setHi(reverseBytes(correlationId.leastSignificantBits)).build(), sent.correlationId)
                assertEquals("operator", sent.causedBy.subject)
                assertEquals("Operator", sent.causedBy.name)
                assertEquals("operator-name", sent.causedBy.userName)
                assertEquals("importer", sent.causedBy.onBehalfOf.subject)
                assertEquals(listOf("Import"), sent.causationList.map { it.type })
                assertEquals(occurred.toString(), sent.causationList.single().occurred.value)
                assertEquals(mapOf("file" to "events.csv"), sent.causationList.single().propertiesMap)
                assertEquals(previousCausation, causationManager.currentChain)
                assertEquals(listOf("{\"value\":\"a\"}", "{\"value\":\"b\"}"), sent.eventsList.map { it.content })
                assertEquals(2, sent.eventsCount)
                sent.eventsList.forEach {
                    assertEquals("BatchEvent", it.eventType.id)
                    assertEquals(2, it.eventType.generation)
                    assertEquals(subject ?: "source-1", it.subject)
                    assertEquals(occurred.toString(), it.occurred.value)
                    assertEquals(listOf("import", "history"), it.tagsList)
                }
            }
        } finally {
            identityProvider.setCurrentIdentity(previousIdentity)
        }
        coVerify(exactly = 3) { stub.appendManyForEventSources(any(), any()) }
        coVerify(exactly = 0) { stub.appendMany(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `routed batch propagates failure to every result without retrying another endpoint`(java: Boolean) = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendManyForEventSources(any(), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addErrors("rejected")).build()
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)
        val options = AppendOptions(eventStreamType = "Orders")
        val events = listOf(BatchEvent("a"), BatchEvent("b"))
        val results = if (java) JavaAppendRoutingUsage.appendMany(sequence, events, options)
            else sequence.appendMany("source-1", events, options)

        assertEquals(2, results.size)
        results.forEach {
            assertFalse(it.isSuccess)
            assertEquals(listOf("rejected"), it.errors.map { error -> error.message })
        }
        coVerify(exactly = 1) { stub.appendManyForEventSources(any(), any()) }
        coVerify(exactly = 0) { stub.appendMany(any(), any()) }
        coVerify(exactly = 0) { stub.append(any(), any()) }
    }
}
