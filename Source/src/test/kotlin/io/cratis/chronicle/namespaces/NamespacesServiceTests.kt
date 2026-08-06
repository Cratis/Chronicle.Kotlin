// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

import Cratis.Chronicle.Contracts.CratisChronicleContracts
import Cratis.Chronicle.Contracts.NamespacesGrpcKt
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamespacesServiceTests {

    @Test
    fun `getAll lists every namespace known to the event store`() = runBlocking {
        val stub = mockk<NamespacesGrpcKt.NamespacesCoroutineStub>()
        val request = slot<CratisChronicleContracts.GetNamespacesRequest>()
        coEvery { stub.getNamespaces(capture(request), any()) } returns CratisChronicleContracts.IEnumerable_String.newBuilder()
            .addAllItems(listOf("Default", "tenant-a"))
            .build()

        val service = NamespacesService("my-event-store", stub)
        val namespaces = service.getAll()

        assertEquals("my-event-store", request.captured.eventStore)
        assertEquals(listOf("Default", "tenant-a"), namespaces)
    }
}
