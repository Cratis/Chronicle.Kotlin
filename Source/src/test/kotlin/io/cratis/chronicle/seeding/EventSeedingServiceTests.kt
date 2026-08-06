// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import Cratis.Chronicle.Contracts.Seeding.EventSeedingGrpcKt
import Cratis.Chronicle.Contracts.Seeding.Seeding
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class CustomerRegistered(val name: String)

private class UnscopedSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventType(CustomerRegistered::class, "customer-1", listOf(CustomerRegistered("Ada")))
    }
}

private class MultiNamespaceSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource("customer-1", listOf(CustomerRegistered("Ada")))
        builder.forNamespace("tenant-a").forEventSource("customer-2", listOf(CustomerRegistered("Grace")))
        builder.forNamespace("tenant-b").forEventSource("customer-3", listOf(CustomerRegistered("Margaret")))
    }
}

class EventSeedingServiceTests {

    @Test
    fun `seed sends everything under the event store's own namespace when forNamespace is never used`() = runBlocking {
        val stub = mockk<EventSeedingGrpcKt.EventSeedingCoroutineStub>()
        val request = slot<Seeding.SeedRequest>()
        coEvery { stub.seed(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = EventSeedingService("my-store", "default", stub)
        service.seed(UnscopedSeeder())

        val namespacedEntries = request.captured.namespacedEntriesList
        assertEquals(1, namespacedEntries.size)
        assertEquals("default", namespacedEntries.single().namespace)
        assertEquals("customer-1", namespacedEntries.single().byEventSourceList.single().eventSourceId)
    }

    @Test
    fun `seed groups entries by namespace when forNamespace targets namespaces other than the ambient one`() = runBlocking {
        val stub = mockk<EventSeedingGrpcKt.EventSeedingCoroutineStub>()
        val request = slot<Seeding.SeedRequest>()
        coEvery { stub.seed(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = EventSeedingService("my-store", "default", stub)
        service.seed(MultiNamespaceSeeder())

        val byNamespace = request.captured.namespacedEntriesList.associateBy { it.namespace }
        assertEquals(3, byNamespace.size)
        assertTrue(byNamespace.containsKey("default"))
        assertTrue(byNamespace.containsKey("tenant-a"))
        assertTrue(byNamespace.containsKey("tenant-b"))

        assertEquals("customer-1", byNamespace.getValue("default").byEventSourceList.single().eventSourceId)
        assertEquals("customer-2", byNamespace.getValue("tenant-a").byEventSourceList.single().eventSourceId)
        assertEquals("customer-3", byNamespace.getValue("tenant-b").byEventSourceList.single().eventSourceId)
    }
}
