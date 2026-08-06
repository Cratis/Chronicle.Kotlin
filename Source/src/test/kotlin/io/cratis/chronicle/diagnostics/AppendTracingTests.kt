// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.diagnostics

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EventType
private data class EmployeeHired(val name: String)

/**
 * Appending is the operation most worth seeing in a trace, so these follow one all the way through
 * the real [EventSequence] rather than testing the tracing helper on its own.
 */
class AppendTracingTests {

    private lateinit var exported: InMemorySpanExporter
    private lateinit var traces: ChronicleTraces

    @BeforeEach
    fun establish() {
        exported = InMemorySpanExporter.create()
        val sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exported))
                    .build()
            )
            .build()
        traces = ChronicleTraces(sdk)
    }

    private fun sequenceFor(stub: EventSequencesGrpcKt.EventSequencesCoroutineStub) =
        EventSequence(EventSequenceId.eventLog, "my-store", "default", stub, traces)

    private fun stubThatAppends(): EventSequencesGrpcKt.EventSequencesCoroutineStub =
        mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>().also {
            coEvery { it.append(any(), any()) } returns
                Eventsequences.AppendResponse.newBuilder().setSequenceNumber(0).build()
        }

    @Test
    fun `an append is a span named after the event type`() = runBlocking {
        sequenceFor(stubThatAppends()).append("employee-1", EmployeeHired("Ada"))

        assertEquals("Chronicle append EmployeeHired", exported.finishedSpanItems.single().name)
    }

    @Test
    fun `the span carries what it takes to find the append again`() = runBlocking {
        sequenceFor(stubThatAppends()).append("employee-1", EmployeeHired("Ada"))

        val attributes = exported.finishedSpanItems.single().attributes
        assertEquals("EmployeeHired", attributes.get(ChronicleTraces.EVENT_TYPE))
        assertEquals("employee-1", attributes.get(ChronicleTraces.EVENT_SOURCE_ID))
        assertEquals(EventSequenceId.eventLog.value, attributes.get(ChronicleTraces.EVENT_SEQUENCE_ID))
        assertEquals("my-store", attributes.get(ChronicleTraces.EVENT_STORE))
        assertEquals("default", attributes.get(ChronicleTraces.NAMESPACE))
    }

    @Test
    fun `a batch is one span carrying how many events it held`() = runBlocking {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendMany(any(), any()) } returns
            Eventsequences.AppendManyResponse.newBuilder().build()

        sequenceFor(stub).appendMany(
            listOf(
                EventForEventSourceId("employee-1", EmployeeHired("Ada")),
                EventForEventSourceId("employee-2", EmployeeHired("Grace"))
            )
        )

        val span = exported.finishedSpanItems.single()
        assertEquals("Chronicle appendMany", span.name)
        assertEquals(2L, span.attributes.get(ChronicleTraces.EVENT_COUNT))
    }

    @Test
    fun `an append the kernel refuses shows up as a failed span`() {
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(any(), any()) } throws IllegalStateException("kernel unreachable")

        assertThrows(IllegalStateException::class.java) {
            runBlocking { sequenceFor(stub).append("employee-1", EmployeeHired("Ada")) }
        }

        assertEquals(StatusCode.ERROR, exported.finishedSpanItems.single().status.statusCode)
    }

    @Test
    fun `an empty batch never reaches the kernel and produces no span`() = runBlocking {
        sequenceFor(mockk()).appendMany(emptyList())

        assertEquals(0, exported.finishedSpanItems.size)
    }
}
