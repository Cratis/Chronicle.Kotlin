// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class OrderPlaced(val id: String)

private data class OrderShipped(val id: String)

private fun Eventsequences.AppendManyRequest.correlationUuid(): UUID =
    UUID(java.lang.Long.reverseBytes(correlationId.lo), java.lang.Long.reverseBytes(correlationId.hi))

/**
 * A composed operation is only observable through what it eventually puts on the wire, so these
 * capture the AppendMany request the client builds from it.
 */
class EventSequenceOperationsTests {

    private fun stubCapturing(
        request: CapturingSlot<Eventsequences.AppendManyRequest>,
        sequenceNumbers: List<Long> = listOf(0L, 1L, 2L)
    ): EventSequencesGrpcKt.EventSequencesCoroutineStub {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(capture(request), any()) } returns Eventsequences.AppendManyResponse.newBuilder()
            .addAllSequenceNumbers(sequenceNumbers)
            .build()
        return stub
    }

    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

    @Test
    fun `perform commits every composed event through a single atomic AppendMany call`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val stub = stubCapturing(request)

        val results = sequenceFor(stub)
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .forEventSourceId("order-2") {
                append(OrderPlaced("order-2"))
                append(OrderShipped("order-2"))
            }
            .perform()

        // The whole point of composing is that it commits as one unit - never one call per source.
        coVerify(exactly = 1) { stub.appendMany(any(), any()) }
        coVerify(exactly = 0) { stub.append(any(), any()) }

        assertEquals(3, request.captured.eventsList.size)
        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `perform sends each event with its own event source id`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()

        sequenceFor(stubCapturing(request))
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .forEventSourceId("order-2") { append(OrderPlaced("order-2")) }
            .perform()

        assertEquals(listOf("order-1", "order-2"), request.captured.eventsList.map { it.eventSourceId })
    }

    @Test
    fun `perform sends a concurrency scope only for the event sources that asked for one`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()

        sequenceFor(stubCapturing(request))
            .forEventSourceId("order-1") {
                withConcurrencyScope { withSequenceNumber(EventSequenceNumber(4)).withEventSourceId() }
                append(OrderPlaced("order-1"))
            }
            .forEventSourceId("order-2") { append(OrderPlaced("order-2")) }
            .perform()

        val scopes = request.captured.concurrencyScopesMap
        assertEquals(setOf("order-1"), scopes.keys)
        assertEquals(4L, scopes["order-1"]?.sequenceNumber)
        assertTrue(scopes["order-1"]?.eventSourceId == true)
    }

    @Test
    fun `perform sends the per-event shaping on the wire`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()

        sequenceFor(stubCapturing(request))
            .forEventSourceId("visit-1") {
                append(OrderPlaced("a"), eventStreamType = "Onboarding", eventStreamId = "2026", tags = listOf("gdpr"))
                append(OrderShipped("a"), subject = "patient-42")
            }
            .perform()

        val first = request.captured.eventsList[0]
        assertEquals("Onboarding", first.eventStreamType)
        assertEquals("2026", first.eventStreamId)
        assertEquals(listOf("gdpr"), first.tagsList)

        val second = request.captured.eventsList[1]
        // Everything unset falls back to the same defaults a plain append uses.
        assertEquals("Default", second.eventStreamType)
        assertEquals("visit-1", second.eventStreamId)
        assertEquals("patient-42", second.subject)
    }

    @Test
    fun `perform sends the correlation id composed onto the operation`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()
        val correlationId = UUID.randomUUID()

        sequenceFor(stubCapturing(request))
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .withCorrelationId(correlationId)
            .perform()

        assertEquals(correlationId, request.captured.correlationUuid())
    }

    @Test
    fun `perform with nothing composed does not call the kernel at all`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()

        val results = sequenceFor(stub).operations().perform()

        assertTrue(results.isEmpty())
        coVerify(exactly = 0) { stub.appendMany(any(), any()) }
    }

    @Test
    fun `configuring the same event source twice adds to what is already staged`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()

        // Composing across call sites is the reason this API exists, so a second visit to the same
        // event source has to accumulate rather than start over.
        sequenceFor(stubCapturing(request))
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .forEventSourceId("order-1") { append(OrderShipped("order-1")) }
            .perform()

        assertEquals(2, request.captured.eventsList.size)
        assertTrue(request.captured.eventsList.all { it.eventSourceId == "order-1" })
    }

    @Test
    fun `getEventsToAppend lists the events in the order they will be appended`() {
        val operations = sequenceFor(mockk())
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .forEventSourceId("order-2") { append(OrderPlaced("order-2")) }
            .forEventSourceId("order-1") { append(OrderShipped("order-1")) }

        assertEquals(
            listOf("order-1" to OrderPlaced("order-1"), "order-1" to OrderShipped("order-1"), "order-2" to OrderPlaced("order-2")),
            operations.getEventsToAppend().map { it.eventSourceId to it.event }
        )
    }

    @Test
    fun `getAppendedEvents returns the events across every event source`() {
        val operations = sequenceFor(mockk())
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .forEventSourceId("order-2") { append(OrderShipped("order-2")) }

        assertEquals(listOf(OrderPlaced("order-1"), OrderShipped("order-2")), operations.getAppendedEvents())
    }

    @Test
    fun `clear removes everything composed so far`() {
        val operations = sequenceFor(mockk())
            .forEventSourceId("order-1") { append(OrderPlaced("order-1")) }
            .withCorrelationId(UUID.randomUUID())

        operations.clear()

        assertTrue(operations.getEventsToAppend().isEmpty())
    }

    @Test
    fun `operations composes against the sequence it was started from`() {
        val sequence = sequenceFor(mockk())
        assertEquals(sequence, sequence.operations().eventSequence)
    }

    @Test
    fun `java composes and performs through the bridges`() {
        val request = slot<Eventsequences.AppendManyRequest>()
        val sequence = sequenceFor(stubCapturing(request, listOf(0L, 1L)))

        val results = JavaEventSequenceOperationsUsage.composeAndPerform(
            sequence,
            OrderPlaced("order-1"),
            OrderShipped("order-2")
        )

        assertEquals(2, results.size)
        assertEquals(listOf("customer-1", "customer-2"), request.captured.eventsList.map { it.eventSourceId })
        assertEquals("Onboarding", request.captured.eventsList[1].eventStreamType)
        assertEquals(setOf("customer-1"), request.captured.concurrencyScopesMap.keys)
        assertEquals(3L, request.captured.concurrencyScopesMap["customer-1"]?.sequenceNumber)
    }

    @Test
    fun `java inspects the staged events without sending them`() {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()

        val staged = JavaEventSequenceOperationsUsage.stagedEvents(sequenceFor(stub), OrderPlaced("order-1"))

        assertEquals(listOf("customer-1"), staged.map { it.eventSourceId })
        coVerify(exactly = 0) { stub.appendMany(any(), any()) }
    }

    @Test
    fun `java appends across event sources through the bridge`() {
        val request = slot<Eventsequences.AppendManyRequest>()
        val sequence = sequenceFor(stubCapturing(request, listOf(0L, 1L)))

        val results = JavaEventSequenceOperationsUsage.appendManyAcrossEventSources(
            sequence,
            OrderPlaced("order-1"),
            OrderShipped("order-2")
        )

        assertEquals(2, results.size)
        assertEquals(listOf("customer-1", "customer-2"), request.captured.eventsList.map { it.eventSourceId })
        // Nothing asked for a concurrency check, so nothing is sent for the kernel to validate.
        assertTrue(request.captured.concurrencyScopesMap.isEmpty())
    }

    @Test
    fun `a composed operation without a concurrency scope leaves every source unchecked`() = runBlocking {
        val request = slot<Eventsequences.AppendManyRequest>()

        sequenceFor(stubCapturing(request))
            .forEventSourceId("order-1") {
                withConcurrencyScope(ConcurrencyScope.notSet)
                append(OrderPlaced("order-1"))
            }
            .perform()

        assertTrue(request.captured.concurrencyScopesMap.isEmpty())
    }
}
