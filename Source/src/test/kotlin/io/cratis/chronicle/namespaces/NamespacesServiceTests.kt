// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

import Cratis.Chronicle.Contracts.Namespaces.NamespacesGrpcKt
import Cratis.Chronicle.Contracts.Namespaces.NamespacesOuterClass
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamespacesServiceTests {

    @Test
    fun `getAll lists every namespace known to the event store`() = runBlocking {
        val stub = mockk<NamespacesGrpcKt.NamespacesCoroutineStub>()
        val request = slot<NamespacesOuterClass.AllNamespacesRequest>()
        every { stub.allNamespaces(capture(request), any()) } returns flowOf(
            NamespacesOuterClass.QueryResult_IEnumerable_String.newBuilder()
                .addAllData(listOf("Default", "tenant-a"))
                .build()
        )

        val service = NamespacesService("my-event-store", stub)
        val namespaces = service.getAll()

        assertEquals("my-event-store", request.captured.eventStore)
        assertEquals(listOf("Default", "tenant-a"), namespaces)
    }
}
