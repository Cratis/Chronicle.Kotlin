// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import Cratis.Chronicle.Contracts.Patterns.PatternsGrpcKt
import Cratis.Chronicle.Contracts.Patterns.PatternsOuterClass
import io.cratis.chronicle.eventSequences.ChronicleCommandRejected
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PatternsTests {

    private val stub = mockk<PatternsGrpcKt.PatternsCoroutineStub>()
    private val patterns = Patterns("my-store", "default", stub)
    private val user = PatternGroupingKey("user-1")

    // A Monday morning in Oslo in winter.
    private val mondayMorning = OffsetDateTime.parse("2026-01-05T09:15+01:00")

    private fun pattern() = PatternsOuterClass.BehaviorPatternDetailsResponse.newBuilder()
        .setId("pattern-1")
        .setGroupingKey("user-1")
        .putFacets("CommandType", "PayInvoice")
        .putFacets("Day", "Monday")
        .setConfidence(0.8)
        .setSupport(0.25)
        .setOccurrences(12)
        .setWeight(3.5)
        .setSpecificity(2)
        .setFirstSeen(PatternsOuterClass.SerializableDateTimeOffset.newBuilder().setValue("2024-01-15T12:30:00.0000000+02:00"))
        .build()

    private fun result(vararg data: PatternsOuterClass.BehaviorPatternDetailsResponse) =
        PatternsOuterClass.QueryResult_IEnumerable_BehaviorPatternDetailsResponse.newBuilder()
            .setIsAuthorized(true)
            .addAllData(data.toList())
            .build()

    private fun usualActions(): CapturingSlot<PatternsOuterClass.UsualActionsRequest> {
        val request = slot<PatternsOuterClass.UsualActionsRequest>()
        coEvery { stub.usualActions(capture(request), any()) } returns result(pattern())
        return request
    }

    @Test
    fun `asking about a moment derives the day and time bucket from it`() = runBlocking {
        val request = usualActions()

        patterns.getPatternsAt(user, mondayMorning)

        assertEquals(mapOf("Day" to "Monday", "TimeBucket" to "Morning"), request.captured.contextMap)
    }

    @Test
    fun `asking about a moment addresses the store, namespace and scope with the server's thresholds`() = runBlocking {
        val request = usualActions()

        patterns.getPatternsAt(user, mondayMorning)

        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
        assertEquals("user-1", request.captured.groupingKey)
        assertEquals(0.0, request.captured.minimumConfidence)
        assertEquals(0, request.captured.maximumResults)
    }

    @Test
    fun `asking without a moment asks about now`() = runBlocking {
        val request = usualActions()

        patterns.getPatternsAt(user)

        assertTrue(request.captured.contextMap.keys.containsAll(listOf("Day", "TimeBucket")))
    }

    @Test
    fun `further facets narrow the question about a moment`() = runBlocking {
        val request = usualActions()

        patterns.getPatternsAt(user, mondayMorning, FacetSet.empty.with(FacetName.AggregateType, "Invoice"))

        assertEquals("Invoice", request.captured.contextMap["AggregateType"])
        assertEquals("Monday", request.captured.contextMap["Day"])
    }

    @Test
    fun `the moment's day wins over a day the caller constrained`() = runBlocking {
        val request = usualActions()

        patterns.getPatternsAt(user, mondayMorning, FacetSet.empty.with(FacetName.Day, "Friday"))

        assertEquals("Monday", request.captured.contextMap["Day"])
    }

    @Test
    fun `the low-level query sends the facet set unchanged`() = runBlocking {
        val request = slot<PatternsOuterClass.MatchingPatternsRequest>()
        coEvery { stub.matchingPatterns(capture(request), any()) } returns result()

        patterns.getPatterns(user, FacetSet.empty.with(FacetName.CommandType, "PayInvoice"), PatternConfidence(0.6), 5)

        assertEquals(mapOf("CommandType" to "PayInvoice"), request.captured.contextMap)
        assertEquals(0.6, request.captured.minimumConfidence)
        assertEquals(5, request.captured.maximumResults)
    }

    @Test
    fun `a returned pattern carries every field the kernel sent`() = runBlocking {
        usualActions()

        val found = patterns.getPatternsAt(user, mondayMorning).single()

        assertEquals(user, found.groupingKey)
        assertEquals(FacetSet.empty.with(FacetName.CommandType, "PayInvoice").with(FacetName.Day, "Monday"), found.facets)
        assertEquals(FacetValue("PayInvoice"), found.action)
        assertEquals(12L, found.occurrences)
        assertEquals(PatternConfidence(0.8), found.confidence)
        assertEquals(0.25, found.support)
        assertEquals(3.5, found.weight)
        assertEquals(OffsetDateTime.parse("2024-01-15T12:30+02:00"), found.firstSeen)
        assertNull(found.lastSeen)
    }

    @Test
    fun `a query the kernel failed throws`() {
        coEvery { stub.patternsForScope(any(), any()) } returns
            PatternsOuterClass.QueryResult_IEnumerable_BehaviorPatternDetailsResponse.newBuilder()
                .setIsAuthorized(true)
                .addExceptionMessages("boom")
                .build()

        assertThrows<ChronicleCommandRejected> { runBlocking { patterns.getPatternsForScope(user) } }
    }

    @Test
    fun `the scopes are the ids the kernel returned`() = runBlocking {
        coEvery { stub.allPatternScopes(any(), any()) } returns
            PatternsOuterClass.QueryResult_IEnumerable_PatternScopeResponse.newBuilder()
                .setIsAuthorized(true)
                .addData(PatternsOuterClass.PatternScopeResponse.newBuilder().setId("user-1").setName("User One"))
                .build()

        assertEquals(listOf(user), patterns.getScopes())
    }
}
