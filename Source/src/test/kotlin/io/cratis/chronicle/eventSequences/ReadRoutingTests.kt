// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@EventType
private data class ReadRoutedEvent(val value: String)

private class ReadRoutedReactor {
    @Suppress("UNUSED_PARAMETER")
    fun handle(event: ReadRoutedEvent) = Unit
}

/**
 * Reads must leave every routing filter unspecified unless the caller narrowed deliberately.
 *
 * This matters more since the kernel owns append routing: an append with no routing options is
 * stored on `Default`/`All`/`Default`, so a read that quietly narrowed to a legacy `Default` stream
 * type would return nothing and look like missing data rather than a filtered query. Empty means
 * "do not narrow" on the kernel's query side, so an empty field on the wire is the correct request.
 */
class ReadRoutingTests {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `the tail read carries no routing filter of its own`(forSingleSource: Boolean) = runBlocking {
        val request = slot<Sequences.TailSequenceNumberRequest>()
        val sequence = sequenceWith(tail = request)

        val tail = if (forSingleSource) sequence.getTailSequenceNumber("source-1")
            else sequence.getTailSequenceNumber()

        assertEquals(41L, tail.value)
        assertEquals(if (forSingleSource) "source-1" else "", request.captured.eventSourceId)
        assertUnnarrowedRoute(
            request.captured.eventSourceType,
            request.captured.eventStreamType,
            request.captured.eventStreamId
        )
        assertEquals("", request.captured.eventTypeIds)
    }

    @Test
    fun `an observer tail narrows to handled event types without narrowing the route`() = runBlocking {
        val request = slot<Sequences.TailSequenceNumberRequest>()
        val sequence = sequenceWith(tail = request)

        assertEquals(41L, sequence.getTailSequenceNumberForObserver(ReadRoutedReactor::class).value)

        assertEquals("ReadRoutedEvent", request.captured.eventTypeIds)
        assertUnnarrowedRoute(
            request.captured.eventSourceType,
            request.captured.eventStreamType,
            request.captured.eventStreamId
        )
    }

    @Test
    fun `reading from a sequence number narrows only by what the caller supplied`() = runBlocking {
        val request = slot<Sequences.FromSequenceNumberRequest>()
        val sequence = sequenceWith(fromSequenceNumber = request)

        sequence.getFromSequenceNumber(EventSequenceNumber.first)
        assertEquals("", request.captured.eventSourceId)
        assertEquals("", request.captured.eventTypeIds)

        sequence.getFromSequenceNumber(EventSequenceNumber(7), "source-1", listOf(ReadRoutedEvent::class))
        assertEquals(7L, request.captured.fromEventSequenceNumber)
        assertEquals("source-1", request.captured.eventSourceId)
        assertEquals("ReadRoutedEvent", request.captured.eventTypeIds)
    }

    @Test
    fun `reading an event source leaves the stream unnarrowed unless narrowing was asked for`() = runBlocking {
        val request = slot<Sequences.ForEventSourceIdAndEventTypesRequest>()
        val sequence = sequenceWith(forEventSource = request)

        sequence.getForEventSourceIdAndEventTypes("source-1", listOf(ReadRoutedEvent::class))
        assertEquals("source-1", request.captured.eventSourceId)
        assertEquals("ReadRoutedEvent", request.captured.eventTypeIds)
        assertUnnarrowedRoute(
            request.captured.eventSourceType,
            request.captured.eventStreamType,
            request.captured.eventStreamId
        )

        sequence.getForEventSourceIdAndEventTypes(
            "source-1",
            listOf(ReadRoutedEvent::class),
            eventStreamType = "Orders",
            eventStreamId = "2026"
        )
        assertEquals("Orders", request.captured.eventStreamType)
        assertEquals("2026", request.captured.eventStreamId)
    }

    /**
     * The event source type was accepted and never written to the request, so a caller narrowing by it
     * got every source type back with no way to tell. The test above exercised this call with a stream
     * scope and never asserted on the source type, which is what kept the drop invisible - an argument
     * exercised but not asserted on. The kernel gained the field in 18.5.0. See Cratis/Chronicle#4049.
     */
    @Test
    fun `reading an event source narrows by the event source type when one is supplied`() = runBlocking {
        val request = slot<Sequences.ForEventSourceIdAndEventTypesRequest>()
        val sequence = sequenceWith(forEventSource = request)

        sequence.getForEventSourceIdAndEventTypes(
            "source-1",
            listOf(ReadRoutedEvent::class),
            eventSourceType = "Order"
        )
        assertEquals("Order", request.captured.eventSourceType)
        assertEquals("", request.captured.eventStreamType)
        assertEquals("", request.captured.eventStreamId)

        sequence.getForEventSourceIdAndEventTypes(
            "source-1",
            listOf(ReadRoutedEvent::class),
            eventStreamType = "Orders",
            eventStreamId = "2026",
            eventSourceType = "Order"
        )
        assertEquals(
            Triple("Order", "Orders", "2026"),
            Triple(
                request.captured.eventSourceType,
                request.captured.eventStreamType,
                request.captured.eventStreamId
            )
        )
    }

    private fun sequenceWith(
        tail: io.mockk.CapturingSlot<Sequences.TailSequenceNumberRequest>? = null,
        fromSequenceNumber: io.mockk.CapturingSlot<Sequences.FromSequenceNumberRequest>? = null,
        forEventSource: io.mockk.CapturingSlot<Sequences.ForEventSourceIdAndEventTypesRequest>? = null
    ): EventSequence {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val events = Sequences.QueryResult_IEnumerable_AppendedEventResponse.newBuilder().setIsAuthorized(true).build()
        tail?.let {
            coEvery { stub.tailSequenceNumber(capture(it), any()) } returns
                Sequences.QueryResult_EventSequenceTailResponse.newBuilder().setIsAuthorized(true)
                    .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(41)).build()
        }
        fromSequenceNumber?.let { coEvery { stub.fromSequenceNumber(capture(it), any()) } returns events }
        forEventSource?.let { coEvery { stub.forEventSourceIdAndEventTypes(capture(it), any()) } returns events }
        return EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)
    }

    /** Empty is the kernel's "do not narrow"; any literal here would hide kernel-defaulted events. */
    private fun assertUnnarrowedRoute(eventSourceType: String, eventStreamType: String, eventStreamId: String) {
        assertEquals(Triple("", "", ""), Triple(eventSourceType, eventStreamType, eventStreamId))
    }
}
