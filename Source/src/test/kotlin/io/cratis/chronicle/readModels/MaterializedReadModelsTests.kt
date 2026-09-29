// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class PagedBook(val title: String)

private fun books(count: Int): List<String> = (0 until count).map { """{"title":"Book $it"}""" }

private fun titles(from: Int, to: Int): List<String> = (from until to).map { "Book $it" }

/** Runs [MaterializedReadModels.getInstances] against a stub returning [returned] and captures the request. */
private fun get(skip: Int, take: Int, returned: List<String>): Pair<Readmodels.GetInstancesRequest, List<String>> {
    val stub = mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>()
    val request = slot<Readmodels.GetInstancesRequest>()
    coEvery { stub.getInstances(capture(request), any()) } returns
        Readmodels.GetInstancesResponse.newBuilder().addAllInstances(returned).build()

    val instances = runBlocking { MaterializedReadModels("store", "default", stub).getInstances(PagedBook::class, skip, take) }

    return request.captured to instances.map { it.title }
}

/** Runs [MaterializedReadModels.observeInstances] against a stub emitting [returned] and captures the request. */
private fun observe(skip: Int, take: Int, returned: List<String>): Pair<Readmodels.ObserveInstancesRequest, List<String>> {
    val stub = mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>()
    val request = slot<Readmodels.ObserveInstancesRequest>()
    every { stub.observeInstances(capture(request), any()) } returns
        flowOf(Readmodels.ObserveInstancesResponse.newBuilder().addAllInstances(returned).build())

    val instances = runBlocking { MaterializedReadModels("store", "default", stub).observeInstances(PagedBook::class, skip, take).first() }

    return request.captured to instances.map { it.title }
}

class MaterializedReadModelsGetInstancesTests {
    @Test
    fun `should request the first zero-based page for a zero skip`() {
        val (request, instances) = get(0, 50, books(3))

        assertEquals(0, request.page)
        assertEquals(50, request.pageSize)
        assertEquals("store", request.eventStore)
        assertEquals("default", request.namespace)
        assertEquals("PagedBook", request.readModel)
        assertEquals(titles(0, 3), instances)
    }

    @Test
    fun `should request the matching zero-based page for a page-aligned skip`() {
        val (request, instances) = get(10, 5, books(5))

        assertEquals(2, request.page)
        assertEquals(5, request.pageSize)
        assertEquals(titles(0, 5), instances)
    }

    @Test
    fun `should request a covering range from the start and slice locally for a non-aligned skip`() {
        val (request, instances) = get(5, 10, books(15))

        assertEquals(0, request.page)
        assertEquals(15, request.pageSize)
        assertEquals(titles(5, 15), instances)
    }

    @Test
    fun `should return only what exists when the covering range is not full`() {
        val (_, instances) = get(5, 10, books(8))

        assertEquals(titles(5, 8), instances)
    }

    @Test
    fun `should request everything from the start and skip locally for an unlimited take`() {
        val (request, instances) = get(3, Int.MAX_VALUE, books(6))

        assertEquals(0, request.page)
        assertEquals(Int.MAX_VALUE, request.pageSize)
        assertEquals(titles(3, 6), instances)
    }

    @Test
    fun `should return nothing for a zero take`() {
        val (request, instances) = get(10, 0, books(4))

        assertEquals(0, request.page)
        assertEquals(0, request.pageSize)
        assertTrue(instances.isEmpty())
    }

    @Test
    fun `should return nothing for a negative take`() {
        val (request, instances) = get(0, -5, books(4))

        assertEquals(0, request.page)
        assertEquals(0, request.pageSize)
        assertTrue(instances.isEmpty())
    }

    @Test
    fun `should treat a negative skip as zero`() {
        val (request, instances) = get(-50, 50, books(3))

        assertEquals(0, request.page)
        assertEquals(50, request.pageSize)
        assertEquals(titles(0, 3), instances)
    }

    @Test
    fun `should treat a negative non-aligned skip as zero`() {
        val (request, instances) = get(-3, 10, books(4))

        assertEquals(0, request.page)
        assertEquals(10, request.pageSize)
        assertEquals(titles(0, 4), instances)
    }
}

class MaterializedReadModelsObserveInstancesTests {
    @Test
    fun `should request the first zero-based page for a zero skip`() {
        val (request, instances) = observe(0, 50, books(3))

        assertEquals(0, request.page)
        assertEquals(50, request.pageSize)
        assertEquals("store", request.eventStore)
        assertEquals("default", request.namespace)
        assertEquals("PagedBook", request.readModel)
        assertEquals(titles(0, 3), instances)
    }

    @Test
    fun `should request the matching zero-based page for a page-aligned skip`() {
        val (request, instances) = observe(10, 5, books(5))

        assertEquals(2, request.page)
        assertEquals(5, request.pageSize)
        assertEquals(titles(0, 5), instances)
    }

    @Test
    fun `should request a covering range from the start and slice locally for a non-aligned skip`() {
        val (request, instances) = observe(5, 10, books(15))

        assertEquals(0, request.page)
        assertEquals(15, request.pageSize)
        assertEquals(titles(5, 15), instances)
    }

    @Test
    fun `should request everything from the start and skip locally for an unlimited take`() {
        val (request, instances) = observe(3, Int.MAX_VALUE, books(6))

        assertEquals(0, request.page)
        assertEquals(Int.MAX_VALUE, request.pageSize)
        assertEquals(titles(3, 6), instances)
    }

    @Test
    fun `should return nothing for a zero take`() {
        val (request, instances) = observe(10, 0, books(4))

        assertEquals(0, request.page)
        assertEquals(0, request.pageSize)
        assertTrue(instances.isEmpty())
    }

    @Test
    fun `should treat a negative skip as zero`() {
        val (request, instances) = observe(-50, 50, books(3))

        assertEquals(0, request.page)
        assertEquals(50, request.pageSize)
        assertEquals(titles(0, 3), instances)
    }
}

class MaterializedPagingTests {
    @Test
    fun `should not overflow the covering page size`() {
        val paging = calculatePaging(Int.MAX_VALUE - 1, 10)

        assertEquals(MaterializedPaging(0, Int.MAX_VALUE, Int.MAX_VALUE - 1, 10), paging)
    }

    @Test
    fun `should keep the requested skip for an unlimited take`() {
        assertEquals(MaterializedPaging(0, Int.MAX_VALUE, 7, Int.MAX_VALUE), calculatePaging(7, Int.MAX_VALUE))
    }

    @Test
    fun `should clamp a negative skip for an unlimited take`() {
        assertEquals(MaterializedPaging(0, Int.MAX_VALUE, 0, Int.MAX_VALUE), calculatePaging(-7, Int.MAX_VALUE))
    }
}
