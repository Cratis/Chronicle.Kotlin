// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.java.AppendOptionsBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class OptionsEventHappened(val value: String)

/**
 * The append-shaping options are only observable on the wire, so these capture the request the
 * client builds. The defaults matter as much as the overrides - appending without options has to
 * keep producing exactly what it always did.
 */
class AppendOptionsTests {

    private fun stubCapturing(request: io.mockk.CapturingSlot<Eventsequences.AppendRequest>):
        EventSequencesGrpcKt.EventSequencesCoroutineStub {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(request), any()) } returns Eventsequences.AppendResponse.newBuilder()
            .setSequenceNumber(0)
            .build()
        return stub
    }

    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "my-store", "default", stub)

    @Test
    fun `append without options keeps the previous defaults`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        sequenceFor(stubCapturing(request)).append("source-1", OptionsEventHappened("hello"))

        assertEquals("Default", request.captured.eventSourceType)
        assertEquals("Default", request.captured.eventStreamType)
        assertEquals("source-1", request.captured.eventStreamId)
        assertEquals("source-1", request.captured.subject)
        assertTrue(request.captured.tagsList.isEmpty())
        assertFalse(request.captured.hasOccurred())
    }

    @Test
    fun `append sends an explicit event source type`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        sequenceFor(stubCapturing(request))
            .append("source-1", OptionsEventHappened("hello"), AppendOptions(eventSourceType = "Patient"))

        assertEquals("Patient", request.captured.eventSourceType)
    }

    @Test
    fun `append sends an explicit event stream type and id`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        sequenceFor(stubCapturing(request)).append(
            "source-1",
            OptionsEventHappened("hello"),
            AppendOptions(eventStreamType = "Onboarding", eventStreamId = "stream-9")
        )

        assertEquals("Onboarding", request.captured.eventStreamType)
        assertEquals("stream-9", request.captured.eventStreamId)
    }

    @Test
    fun `append sends an explicit subject`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        sequenceFor(stubCapturing(request))
            .append("visit-1", OptionsEventHappened("hello"), AppendOptions(subject = "patient-42"))

        // The subject is what PII is held against, so it has to be settable independently of the
        // event source - a visit is not the person the data is about.
        assertEquals("patient-42", request.captured.subject)
        assertEquals("visit-1", request.captured.eventSourceId)
    }

    @Test
    fun `append sends tags`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        sequenceFor(stubCapturing(request))
            .append("source-1", OptionsEventHappened("hello"), AppendOptions(tags = listOf("gdpr", "import")))

        assertEquals(listOf("gdpr", "import"), request.captured.tagsList)
    }

    @Test
    fun `append sends an explicit occurred`() = runBlocking {
        val request = slot<Eventsequences.AppendRequest>()
        val occurred = Instant.parse("2020-03-01T10:15:30Z")
        sequenceFor(stubCapturing(request))
            .append("source-1", OptionsEventHappened("hello"), AppendOptions(occurred = occurred))

        assertTrue(request.captured.hasOccurred())
        assertEquals(occurred, Instant.parse(request.captured.occurred.value))
    }

    @Test
    fun `appendMany applies the options to every event in the batch`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Eventsequences.AppendManyRequest>()
        coEvery { stub.appendMany(capture(request), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().addSequenceNumbers(0).addSequenceNumbers(1).build()

        sequenceFor(stub).appendMany(
            "source-1",
            listOf(OptionsEventHappened("one"), OptionsEventHappened("two")),
            AppendOptions(subject = "patient-42", tags = listOf("gdpr"), eventStreamType = "Onboarding")
        )

        assertEquals(2, request.captured.eventsList.size)
        request.captured.eventsList.forEach { event ->
            assertEquals("patient-42", event.subject)
            assertEquals(listOf("gdpr"), event.tagsList)
            assertEquals("Onboarding", event.eventStreamType)
        }
    }

    @Test
    fun `appendMany without options keeps the previous defaults`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val request = slot<Eventsequences.AppendManyRequest>()
        coEvery { stub.appendMany(capture(request), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().addSequenceNumbers(0).build()

        sequenceFor(stub).appendMany("source-1", listOf(OptionsEventHappened("one")))

        val event = request.captured.eventsList.single()
        assertEquals("Default", event.eventSourceType)
        assertEquals("Default", event.eventStreamType)
        assertEquals("source-1", event.eventStreamId)
        assertEquals("source-1", event.subject)
        assertTrue(event.tagsList.isEmpty())
    }

    @Test
    fun `the java builder sets only what it is given`() {
        val options = AppendOptionsBuilder()
            .subject("patient-42")
            .tag("gdpr")
            .build()

        assertEquals("patient-42", options.subject)
        assertEquals(listOf("gdpr"), options.tags)
        // Everything untouched stays null so the append falls back to its defaults.
        assertEquals(null, options.eventStreamType)
        assertEquals(null, options.occurred)
        assertEquals(null, options.correlationId)
    }

    @Test
    fun `the java builder accumulates tags`() {
        val options = AppendOptionsBuilder().tag("one").tags(listOf("two", "three")).build()
        assertEquals(listOf("one", "two", "three"), options.tags)
    }

    @Test
    fun `java keeps the positional constructors it already compiles against`() {
        // Adding a property to the data class silently breaks Java callers using a positional
        // constructor, because Kotlin default arguments do not exist there. JavaAppendOptionsUsage
        // fails to compile if that happens, and these assert the values still land where expected.
        assertEquals(null, JavaAppendOptionsUsage.empty().subject)

        val correlationId = java.util.UUID.randomUUID()
        val scope = ConcurrencyScope(EventSequenceNumber(3), eventSourceId = true)
        val options = JavaAppendOptionsUsage.withCorrelationAndScope(correlationId, scope)
        assertEquals(correlationId, options.correlationId)
        assertEquals(scope, options.concurrencyScope)
    }

    @Test
    fun `java reaches the new options through the builder`() {
        val options = JavaAppendOptionsUsage.viaBuilder("patient-42", "gdpr")
        assertEquals("patient-42", options.subject)
        assertEquals(listOf("gdpr"), options.tags)
    }
}
