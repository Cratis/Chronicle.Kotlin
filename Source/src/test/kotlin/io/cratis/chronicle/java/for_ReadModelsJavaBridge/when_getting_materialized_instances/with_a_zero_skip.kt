// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.when_getting_materialized_instances

import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import io.cratis.chronicle.java.ReadModelsJavaBridge
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.Book
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.MaterializedReadModels
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WithAZeroSkip {
    @Test
    fun `should request the first zero-based page from the kernel`() {
        val stub = mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>()
        val request = slot<Readmodels.GetInstancesRequest>()
        coEvery { stub.getInstances(capture(request), any()) } returns
            Readmodels.GetInstancesResponse.newBuilder().addInstances("""{"title":"Dune"}""").build()
        val service = mockk<IReadModelsService>()
        every { service.materialized } returns MaterializedReadModels("store", "default", stub)

        val instances = ReadModelsJavaBridge.getMaterializedInstances(service, Book::class.java, 0, 50)

        assertEquals(0, request.captured.page)
        assertEquals(50, request.captured.pageSize)
        assertEquals(listOf(Book("Dune")), instances)
    }
}
