// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.diagnostics

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The client reports spans through the OpenTelemetry API, which no-ops until an application installs
 * an SDK. These run with one installed, so what an instrumented application would actually see is
 * what is asserted on.
 */
class ChronicleTracesTests {

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

    @Test
    fun `a span is reported for the work it wrapped`() = runBlocking {
        traces.span("Chronicle append EmployeeHired") { "done" }

        val span = exported.finishedSpanItems.single()
        assertEquals("Chronicle append EmployeeHired", span.name)
        assertEquals(StatusCode.UNSET, span.status.statusCode)
    }

    @Test
    fun `what the block returned is what comes back`() = runBlocking {
        assertEquals("done", traces.span("anything") { "done" })
    }

    @Test
    fun `the attributes given are on the span`() = runBlocking {
        traces.span(
            "Chronicle append EmployeeHired",
            Attributes.of(
                ChronicleTraces.EVENT_TYPE, "EmployeeHired",
                ChronicleTraces.EVENT_SOURCE_ID, "employee-1"
            )
        ) { }

        val attributes = exported.finishedSpanItems.single().attributes
        assertEquals("EmployeeHired", attributes.get(ChronicleTraces.EVENT_TYPE))
        assertEquals("employee-1", attributes.get(ChronicleTraces.EVENT_SOURCE_ID))
    }

    @Test
    fun `a failure is recorded on the span and still thrown`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { traces.span("Chronicle append EmployeeHired") { throw IllegalStateException("kernel said no") } }
        }

        val span = exported.finishedSpanItems.single()
        assertEquals(StatusCode.ERROR, span.status.statusCode)
        assertEquals("kernel said no", span.status.description)
        assertTrue(span.events.any { it.name == "exception" })
    }

    @Test
    fun `work done inside a span nests underneath it`() = runBlocking {
        traces.span("Chronicle append EmployeeHired") {
            traces.span("Chronicle observe EmployeeHired") { }
        }

        val spans = exported.finishedSpanItems
        val outer = spans.single { it.name == "Chronicle append EmployeeHired" }
        val inner = spans.single { it.name == "Chronicle observe EmployeeHired" }
        assertEquals(outer.spanId, inner.parentSpanId)
    }

    @Test
    fun `with no sdk installed nothing is recorded and the work still runs`() = runBlocking {
        // This is what an application that never instruments gets: the API's no-op tracer.
        var ran = false
        val result = ChronicleTraces(io.opentelemetry.api.OpenTelemetry.noop()).span("anything") {
            ran = true
            "done"
        }

        assertTrue(ran)
        assertEquals("done", result)
        assertTrue(exported.finishedSpanItems.isEmpty())
    }
}
