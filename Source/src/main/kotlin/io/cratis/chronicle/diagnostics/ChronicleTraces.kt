// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.diagnostics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context

/**
 * The spans the client produces, so Chronicle work shows up in an application's traces.
 *
 * Appending an event, observing one in a reactor, folding one in a reducer - each becomes a span
 * with the identifiers you would otherwise have to correlate by hand: which event type, which event
 * source, which observer, which position in the sequence.
 *
 * Nothing has to be turned on. The client depends on the OpenTelemetry *API*, which no-ops until an
 * application registers an SDK, so an application that does not instrument pays a virtual call and
 * nothing else. One that does gets Chronicle spans nested under whatever span it was already in.
 *
 * @param openTelemetry Where the tracer comes from. Defaults to whatever the application registered
 *   globally, resolved on first use rather than at construction so that a client built before the
 *   SDK is installed still ends up with the real one.
 */
class ChronicleTraces(private val openTelemetry: OpenTelemetry? = null) {

    private val tracer: Tracer by lazy {
        (openTelemetry ?: GlobalOpenTelemetry.get()).getTracer(INSTRUMENTATION_NAME)
    }

    /**
     * Runs [block] inside a span named [name].
     *
     * The span is closed whether [block] returns or throws, and a throw is recorded on it - a failed
     * append or a reactor that blew up is exactly what someone reading a trace is looking for.
     *
     * The span is made current for the duration, so anything [block] does that is itself instrumented
     * - an HTTP call out of a reactor, say - nests underneath rather than floating at the root.
     */
    suspend fun <T> span(name: String, attributes: Attributes = Attributes.empty(), block: suspend () -> T): T {
        val span = tracer.spanBuilder(name)
            .setSpanKind(SpanKind.INTERNAL)
            .setAllAttributes(attributes)
            .startSpan()

        val scope = span.makeCurrent()
        try {
            return block()
        } catch (e: Throwable) {
            span.setStatus(StatusCode.ERROR, e.message ?: e::class.simpleName ?: "failed")
            span.recordException(e)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }

    /** Whether anything is actually recording, so callers can skip building attributes for nothing. */
    val isRecording: Boolean get() = io.opentelemetry.api.trace.Span.fromContext(Context.current()).isRecording

    companion object {
        /** The instrumentation scope every Chronicle span is reported under. */
        const val INSTRUMENTATION_NAME: String = "io.cratis.chronicle"

        /** The event type being appended or observed. */
        val EVENT_TYPE: AttributeKey<String> = AttributeKey.stringKey("chronicle.event_type")

        /** The event source the event belongs to. */
        val EVENT_SOURCE_ID: AttributeKey<String> = AttributeKey.stringKey("chronicle.event_source_id")

        /** The event sequence being appended to or observed. */
        val EVENT_SEQUENCE_ID: AttributeKey<String> = AttributeKey.stringKey("chronicle.event_sequence_id")

        /** The event store the work belongs to. */
        val EVENT_STORE: AttributeKey<String> = AttributeKey.stringKey("chronicle.event_store")

        /** The namespace within the event store. */
        val NAMESPACE: AttributeKey<String> = AttributeKey.stringKey("chronicle.namespace")

        /** The observer handling the event. */
        val OBSERVER_ID: AttributeKey<String> = AttributeKey.stringKey("chronicle.observer_id")

        /** The position of the event in its sequence. */
        val SEQUENCE_NUMBER: AttributeKey<Long> = AttributeKey.longKey("chronicle.sequence_number")

        /** How many events a batch carries. */
        val EVENT_COUNT: AttributeKey<Long> = AttributeKey.longKey("chronicle.event_count")

        /** Whether the event arrived as part of a replay. */
        val IS_REPLAY: AttributeKey<Boolean> = AttributeKey.booleanKey("chronicle.is_replay")

        /** Spans produced when nothing was configured, which is every client that never asked. */
        val default: ChronicleTraces = ChronicleTraces()
    }
}
