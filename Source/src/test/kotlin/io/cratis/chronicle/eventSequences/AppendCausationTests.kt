// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.identity.Identity
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.util.Collections
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

@EventType
private data class CausationTestEvent(val value: String)

class AppendCausationTests {
    private val causation = listOf(
        Causation(Instant.parse("2020-03-01T10:15:30Z"), CausationType("LegacyImport"), mapOf("file" to "1998.csv"))
    )
    private val identity = Identity("user-42", "Ada", "ada")

    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

    @Test
    fun `append maps the explicit operation context exactly`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(request), any()) } returns
            Eventsequences.AppendResponse.newBuilder().setSequenceNumber(0).build()
        val correlationId = UUID.randomUUID()

        withContext(Dispatchers.Default) {
            sequenceFor(stub).append(
                "source-1",
                CausationTestEvent("hello"),
                OperationContext(correlationId, causation, identity)
            )
        }

        assertEquals(correlationId, request.captured.correlationId.toUuid())
        assertEquals(listOf("LegacyImport"), request.captured.causationList.map { it.type })
        assertEquals(mapOf("file" to "1998.csv"), request.captured.causationList.single().propertiesMap)
        assertEquals("user-42", request.captured.causedBy.subject)
    }

    @Test
    fun `append many uses one exact context for the whole batch`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(capture(request), any()) } returns Eventsequences.AppendManyResponse.getDefaultInstance()
        val context = OperationContext(UUID.randomUUID(), causation, identity)

        sequenceFor(stub).appendMany(
            listOf(
                EventForEventSourceId("source-1", CausationTestEvent("a")),
                EventForEventSourceId("source-2", CausationTestEvent("b"))
            ),
            context
        )

        assertEquals(context.correlationId, request.captured.correlationId.toUuid())
        assertEquals(listOf("LegacyImport"), request.captured.causationList.map { it.type })
        assertEquals("user-42", request.captured.causedBy.subject)
        assertEquals(2, request.captured.eventsCount)
    }

    @Test
    fun `parallel convenience calls receive distinct contexts without contamination`() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<Eventsequences.AppendRequest>())
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(requests), any()) } returns
            Eventsequences.AppendResponse.newBuilder().setSequenceNumber(0).build()
        val sequence = sequenceFor(stub)

        (1..20).map { index ->
            async(Dispatchers.Default) { sequence.append("source-$index", CausationTestEvent(index.toString())) }
        }.awaitAll()

        val ids = requests.map { it.correlationId.toUuid() }
        assertEquals(20, ids.toSet().size)
        assertEquals(setOf("[System]"), requests.map { it.causedBy.name }.toSet())
    }

    @Test
    fun `system factory creates a fresh context per call`() {
        assertNotEquals(OperationContext.system().correlationId, OperationContext.system().correlationId)
    }

    private fun bcl.Bcl.Guid.toUuid(): UUID =
        UUID(java.lang.Long.reverseBytes(lo), java.lang.Long.reverseBytes(hi))
}
