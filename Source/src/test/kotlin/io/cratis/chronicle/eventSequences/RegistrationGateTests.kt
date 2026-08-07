// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class GatedEvent(val value: String = "")

/**
 * Registration happens on connect, in the background, so an append issued straight after
 * `getEventStore` would race the event type registration it depends on. The kernel rejects an event
 * of a type it has never been told about, and that is not a race an application should have to know
 * about - so the append waits.
 */
class RegistrationGateTests {

    private fun stubThatAppends() = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>().also {
        coEvery { it.append(any(), any()) } returns
            Eventsequences.AppendResponse.newBuilder().setSequenceNumber(0).build()
        coEvery { it.appendMany(any(), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().build()
    }

    private fun sequenceGatedBy(gate: IRegistrationGate) = EventSequence(
        EventSequenceId.eventLog,
        "my-store",
        "default",
        stubThatAppends(),
        io.cratis.chronicle.diagnostics.ChronicleTraces.default,
        gate
    )

    @Test
    fun `an append waits for the first registration pass`() = runBlocking {
        val opened = CompletableDeferred<Unit>()
        val sequence = sequenceGatedBy { opened.await() }

        val append = async { sequence.append("source-1", GatedEvent("hello")) }
        repeat(5) { yield() }

        assertFalse(append.isCompleted, "the append reached the kernel before registration finished")

        opened.complete(Unit)
        assertEquals(EventSequenceNumber(0), withTimeout(5_000) { append.await() }.sequenceNumber)
    }

    @Test
    fun `a batch waits for it too`() = runBlocking {
        val opened = CompletableDeferred<Unit>()
        val sequence = sequenceGatedBy { opened.await() }

        val append = async { sequence.appendMany(listOf(EventForEventSourceId("source-1", GatedEvent("hello")))) }
        repeat(5) { yield() }

        assertFalse(append.isCompleted)

        opened.complete(Unit)
        withTimeout(5_000) { append.await() }
    }

    @Test
    fun `an open gate costs the append nothing`() = runBlocking {
        val result = withTimeout(5_000) {
            sequenceGatedBy(IRegistrationGate.open).append("source-1", GatedEvent("hello"))
        }

        assertEquals(EventSequenceNumber(0), result.sequenceNumber)
    }

    @Test
    fun `an empty batch never waits, because it never reaches the kernel`() = runBlocking {
        // Nothing to register against, so gating it would be a pointless wait on an operation that
        // does nothing.
        val results = withTimeout(1_000) {
            sequenceGatedBy { delay(Long.MAX_VALUE) }.appendMany(emptyList())
        }

        assertTrue(results.isEmpty())
    }

    @Test
    fun `waiting happens once, not once per append`() = runBlocking {
        var waits = 0
        val sequence = sequenceGatedBy { waits++ }

        sequence.append("source-1", GatedEvent("one"))
        sequence.append("source-1", GatedEvent("two"))

        // The gate is asked every time - it is what makes the check cheap rather than stateful - but
        // it must not be doing work on the second call. Whatever backs it has already completed.
        assertEquals(2, waits)
    }
}
