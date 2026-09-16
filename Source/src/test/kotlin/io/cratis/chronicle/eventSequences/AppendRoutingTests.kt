// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private data class RoutedEvent(val value: String)

class AppendRoutingTests {
    private val routes = listOf(
        null,
        AppendOptions(),
        AppendOptions(eventSourceType = "", eventStreamType = "", eventStreamId = ""),
        AppendOptions(eventSourceType = "Default", eventStreamType = "Default", eventStreamId = "source-1"),
        AppendOptions(eventSourceType = "Account", eventStreamType = "Orders", eventStreamId = "2026"),
        AppendOptions(eventSourceType = "Account"),
        AppendOptions(eventStreamType = "Orders"),
        AppendOptions(eventStreamId = "2026")
    )
    private val scopes = listOf(null, ConcurrencyScope.none, ConcurrencyScope.notSet,
        ConcurrencyScope(EventSequenceNumber(12), eventSourceId = true,
            eventStreamType = "CheckedStream", eventStreamId = "checked-1", eventSourceType = "CheckedSource",
            eventTypes = listOf(EventTypeDescriptor(EventTypeId("CheckedEvent"), EventTypeGeneration(3), true))))

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `single append forwards routing verbatim independently of concurrency`(java: Boolean) = runBlocking {
        val request = slot<Sequences.AppendRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.append(capture(request), any()) } returns Sequences.CommandResult_AppendResponse.newBuilder()
            .setIsAuthorized(true).setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(0)).build()
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)

        for (route in routes) for (scope in scopes) {
            val options = route?.copy(concurrencyScope = scope) ?: scope?.let { AppendOptions(concurrencyScope = it) }
            if (java) JavaAppendRoutingUsage.append(sequence, RoutedEvent("single"), options)
            else sequence.append("source-1", RoutedEvent("single"), options)

            assertEquals(route?.eventSourceType.orEmpty(), request.captured.eventSourceType)
            assertEquals(route?.eventStreamType.orEmpty(), request.captured.eventStreamType)
            assertEquals(route?.eventStreamId.orEmpty(), request.captured.eventStreamId)
            assertEquals("source-1", request.captured.subject)
            assertScope(scope ?: ConcurrencyScope.none, request.captured.concurrencyScope)
        }
        coVerify(exactly = 32) { stub.append(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `single source batch uses rich endpoint for any supplied route and preserves scope`(java: Boolean) = runBlocking {
        val plain = slot<Sequences.AppendManyRequest>()
        val rich = slot<Sequences.AppendManyForEventSourcesRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        val response = Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
            .setResponse(Sequences.AppendManyResponse.newBuilder().addSequenceNumbers(0).addSequenceNumbers(1)).build()
        coEvery { stub.appendMany(capture(plain), any()) } returns response
        coEvery { stub.appendManyForEventSources(capture(rich), any()) } returns response
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)
        val events = listOf(RoutedEvent("a"), RoutedEvent("b"))

        for ((index, route) in routes.withIndex()) for (scope in scopes) {
            val options = route?.copy(concurrencyScope = scope) ?: scope?.let { AppendOptions(concurrencyScope = it) }
            val results = if (java) JavaAppendRoutingUsage.appendMany(sequence, events, options)
                else sequence.appendMany("source-1", events, options)
            assertEquals(listOf(0L, 1L), results.map { it.sequenceNumber.value })
            assertTrue(results.all { it.isSuccess })
            if (index < 2) {
                assertEquals(2, plain.captured.eventsCount)
                assertEquals("source-1", plain.captured.eventSourceId)
                assertScope(scope ?: ConcurrencyScope.none, plain.captured.concurrencyScope)
            } else {
                assertEquals(2, rich.captured.eventsCount)
                for (event in rich.captured.eventsList) {
                    assertEquals(route?.eventSourceType.orEmpty(), event.eventSourceType)
                    assertEquals(route?.eventStreamType.orEmpty(), event.eventStreamType)
                    assertEquals(route?.eventStreamId.orEmpty(), event.eventStreamId)
                    assertEquals("source-1", event.subject)
                }
                assertEquals("source-1", rich.captured.concurrencyScopesList.single().eventSourceId)
                assertScope(scope ?: ConcurrencyScope.none, rich.captured.concurrencyScopesList.single().scope)
            }
        }
        coVerify(exactly = 8) { stub.appendMany(any(), any()) }
        coVerify(exactly = 24) { stub.appendManyForEventSources(any(), any()) }
        coVerify(exactly = 0) { stub.append(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `rich mixed route batch preserves each route and sparse concurrency map`(java: Boolean) = runBlocking {
        val request = slot<Sequences.AppendManyForEventSourcesRequest>()
        val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
        coEvery { stub.appendManyForEventSources(capture(request), any()) } returns
            Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().addAllSequenceNumbers((0L..7L).toList())).build()
        val sequence = EventSequence(EventSequenceId.eventLog, "store", "tenant", stub)
        val events = routes.mapIndexed { index, options ->
            EventForEventSourceId("source-$index", RoutedEvent("$index"),
                eventSourceType = options?.eventSourceType, eventStreamType = options?.eventStreamType,
                eventStreamId = options?.eventStreamId, subject = if (index == 0) "" else null)
        }
        val scopesBySource = mapOf("source-0" to ConcurrencyScope.none, "source-1" to ConcurrencyScope.notSet,
            "source-2" to scopes.last()!!)
        if (java) JavaAppendRoutingUsage.appendMixed(sequence, events, scopesBySource, null)
        else sequence.appendMany(events, scopesBySource)

        assertEquals(8, request.captured.eventsCount)
        events.zip(request.captured.eventsList).forEach { (expected, actual) ->
            assertEquals(expected.eventSourceId, actual.eventSourceId)
            assertEquals(expected.eventSourceType.orEmpty(), actual.eventSourceType)
            assertEquals(expected.eventStreamType.orEmpty(), actual.eventStreamType)
            assertEquals(expected.eventStreamId.orEmpty(), actual.eventStreamId)
            assertEquals(expected.subject ?: expected.eventSourceId, actual.subject)
        }
        assertEquals(scopesBySource.keys, request.captured.concurrencyScopesList.map { it.eventSourceId }.toSet())
        request.captured.concurrencyScopesList.forEach { assertScope(scopesBySource.getValue(it.eventSourceId), it.scope) }
        assertFalse(request.captured.eventsList[0].hasOccurred())
        coVerify(exactly = 1) { stub.appendManyForEventSources(any(), any()) }
    }

    private fun assertScope(expected: ConcurrencyScope, actual: Sequences.ConcurrencyScope) {
        assertEquals(expected.sequenceNumber.value, actual.sequenceNumber)
        assertEquals(expected.eventSourceId, actual.eventSourceId)
        assertEquals(expected.eventSourceType.orEmpty(), actual.eventSourceType)
        assertEquals(expected.eventStreamType.orEmpty(), actual.eventStreamType)
        assertEquals(expected.eventStreamId.orEmpty(), actual.eventStreamId)
        assertEquals(expected.eventTypes.map { Triple(it.id.value, it.generation.value, it.tombstone) },
            actual.eventTypesList.map { Triple(it.id, it.generation, it.tombstone) })
    }
}
