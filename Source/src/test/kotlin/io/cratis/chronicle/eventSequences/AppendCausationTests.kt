// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.events.EventType
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@EventType
private data class CausationTestEvent(val value: String)

/**
 * Causation is ambient by default - taken from the chain the current thread has built up - which is
 * what nearly every append wants. These pin the override that lets an append attribute itself to
 * something else, and the one shape the wire cannot carry.
 */
class AppendCausationTests {

    private val imported = listOf(
        Causation(Instant.parse("2020-03-01T10:15:30Z"), CausationType("LegacyImport"), mapOf("file" to "1998.csv"))
    )

    private fun stubReturningOneAppend(request: CapturingSlot<Eventsequences.AppendRequest>) =
        mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>().also { stub ->
            coEvery { stub.append(capture(request), any()) } returns
                Eventsequences.AppendResponse.newBuilder().setSequenceNumber(0).build()
        }

    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

    @Test
    fun `an append with no override carries the ambient chain`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val sequence = sequenceFor(stubReturningOneAppend(request))

        sequence.append("source-1", CausationTestEvent("hello"))

        val types = request.captured.causationList.map { it.type }
        assertTrue(types.contains(CausationType.appendEvent.name), "expected the ambient append entry, got $types")
    }

    @Test
    fun `an append with an override carries that chain instead`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val sequence = sequenceFor(stubReturningOneAppend(request))

        sequence.append("source-1", CausationTestEvent("hello"), AppendOptions(causation = imported))

        assertEquals(listOf("LegacyImport"), request.captured.causationList.map { it.type })
        assertEquals(mapOf("file" to "1998.csv"), request.captured.causationList.single().propertiesMap)
    }

    @Test
    fun `an override leaves the ambient chain alone`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val sequence = sequenceFor(stubReturningOneAppend(request))
        causationManager.clear()
        val before = causationManager.currentChain

        sequence.append("source-1", CausationTestEvent("hello"), AppendOptions(causation = imported))

        assertEquals(before, causationManager.currentChain)
    }

    @Test
    fun `a batch carries the causation its events agree on`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(capture(request), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().build()

        sequenceFor(stub).appendMany(
            listOf(
                EventForEventSourceId("source-1", CausationTestEvent("a"), causation = imported),
                EventForEventSourceId("source-2", CausationTestEvent("b"), causation = imported)
            )
        )

        assertEquals(listOf("LegacyImport"), request.captured.causationList.map { it.type })
    }

    @Test
    fun `a batch whose events disagree on causation is rejected rather than losing one of them`() {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val sequence = sequenceFor(stub)

        val error = assertThrows(CausationDiffersAcrossBatch::class.java) {
            runBlocking {
                sequence.appendMany(
                    listOf(
                        EventForEventSourceId("source-1", CausationTestEvent("a"), causation = imported),
                        EventForEventSourceId(
                            "source-2",
                            CausationTestEvent("b"),
                            causation = listOf(Causation(Instant.EPOCH, CausationType("SomethingElse")))
                        )
                    )
                )
            }
        }

        assertTrue(error.message!!.contains("one chain"))
    }

    @Test
    fun `a batch with no override carries the ambient chain`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(capture(request), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().build()

        sequenceFor(stub).appendMany(
            listOf(EventForEventSourceId("source-1", CausationTestEvent("a")))
        )

        val types = request.captured.causationList.map { it.type }
        assertTrue(types.contains(CausationType.appendManyEvents.name), "expected the ambient batch entry, got $types")
    }

    @Test
    fun `the single source batch form passes its causation through`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(capture(request), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().build()

        sequenceFor(stub).appendMany(
            "source-1",
            listOf(CausationTestEvent("a"), CausationTestEvent("b")),
            AppendOptions(causation = imported)
        )

        assertEquals(listOf("LegacyImport"), request.captured.causationList.map { it.type })
    }
}
