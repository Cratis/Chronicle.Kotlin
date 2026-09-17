// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@EventType
private data class RouteConformanceEvent(val number: Int)

/** Golden kernel routing examples exercised through the public server-double append surface. */
class InMemoryRoutingConformanceTests {
    enum class AppendShape { Single, SingleSourceBatch, RichBatch }

    private val cases = listOf(
        null to Triple("Default", "All", "Default"),
        AppendOptions() to Triple("Default", "All", "Default"),
        AppendOptions(eventSourceType = "", eventStreamType = "", eventStreamId = "") to Triple("Default", "All", "Default"),
        AppendOptions(eventSourceType = "Default", eventStreamType = "Default", eventStreamId = "source-1") to Triple("Default", "Default", "source-1"),
        AppendOptions(eventSourceType = "Account", eventStreamType = "Orders", eventStreamId = "2026") to Triple("Account", "Orders", "2026"),
        AppendOptions(eventSourceType = "Account", eventStreamType = "") to Triple("Account", "All", "Default"),
        AppendOptions(eventStreamType = "Orders", eventStreamId = "") to Triple("Default", "Orders", "Default"),
        AppendOptions(eventStreamId = "source-1") to Triple("Default", "All", "source-1"),
        AppendOptions(eventSourceType = " ", eventStreamType = " ", eventStreamId = " ") to Triple(" ", " ", " ")
    )

    @ParameterizedTest
    @EnumSource(AppendShape::class)
    fun `all append shapes emulate kernel missing empty explicit and mixed routing`(shape: AppendShape) = runBlocking {
        val sequence = InMemoryEventSequence(eventStoreName = "store", namespace = "tenant")
        when (shape) {
            AppendShape.Single -> cases.forEachIndexed { index, (options, _) ->
                assertTrue(sequence.append("source-1", RouteConformanceEvent(index), options).isSuccess)
            }
            AppendShape.SingleSourceBatch -> cases.forEachIndexed { index, (options, _) ->
                val results = sequence.appendMany("source-1", listOf(RouteConformanceEvent(index)), options)
                assertEquals(1, results.size)
                assertTrue(results.single().isSuccess)
            }
            AppendShape.RichBatch -> {
                val events = cases.mapIndexed { index, (options, _) ->
                    EventForEventSourceId(
                        eventSourceId = "source-1", event = RouteConformanceEvent(index),
                        eventSourceType = options?.eventSourceType,
                        eventStreamType = options?.eventStreamType, eventStreamId = options?.eventStreamId
                    )
                }
                val results = sequence.appendMany(events)
                assertEquals(9, results.size)
                assertTrue(results.all { it.isSuccess })
            }
        }
        assertEquals(9, sequence.events.size)
        cases.zip(sequence.events).forEachIndexed { index, (case, event) ->
            val expected = case.second
            val context = event.context
            assertEquals(expected, Triple(context.eventSourceType, context.eventStreamType, context.eventStreamId), "case $index")
            assertEquals("source-1", context.eventSourceId)
            assertEquals("store", context.eventStore)
            assertEquals("tenant", context.namespace)
            assertEquals(index.toLong(), context.sequenceNumber)
        }
    }

    @ParameterizedTest
    @EnumSource(AppendShape::class)
    fun `explicit concurrency scopes do not supply routing defaults in the server double`(shape: AppendShape) = runBlocking {
        val sequence = InMemoryEventSequence()
        // Scope enforcement is outside this double's contract. Scope dimensions must not become routes.
        val scope = ConcurrencyScope.none.copy(eventSourceType = "CheckedSource", eventStreamType = "CheckedStream", eventStreamId = "checked-id")
        val event = RouteConformanceEvent(0)
        when (shape) {
            AppendShape.Single -> sequence.append("source-1", event, AppendOptions(concurrencyScope = scope))
            AppendShape.SingleSourceBatch -> sequence.appendMany("source-1", listOf(event), AppendOptions(concurrencyScope = scope))
            AppendShape.RichBatch -> sequence.appendMany(listOf(EventForEventSourceId("source-1", event)), mapOf("source-1" to scope))
        }
        assertEquals(1, sequence.events.size)
        val context = sequence.events.single().context
        assertEquals(Triple("Default", "All", "Default"), Triple(context.eventSourceType, context.eventStreamType, context.eventStreamId))
    }
}
