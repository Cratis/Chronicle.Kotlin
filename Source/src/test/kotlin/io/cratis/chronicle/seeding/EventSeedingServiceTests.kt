// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import Cratis.Chronicle.Contracts.Seeding.EventSeedingGrpcKt
import Cratis.Chronicle.Contracts.Seeding.Seeding
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
    fun `seed sends entries without forNamespace as global seed data`() = runBlocking {
        val stub = mockk<EventSeedingGrpcKt.EventSeedingCoroutineStub>()
        val request = slot<Seeding.SeedEventsRequest>()
        coEvery { stub.seedEvents(capture(request), any()) } returns
            Seeding.CommandResult.newBuilder().setIsAuthorized(true).build()

        val service = EventSeedingService("my-store", "default", stub)
        service.seed(UnscopedSeeder())

        assertTrue(request.captured.namespacedEntriesList.isEmpty())
        val byEventSource = request.captured.globalByEventSourceList.single()
        assertEquals("customer-1", byEventSource.eventSourceId)
        assertEquals("CustomerRegistered", byEventSource.entriesList.single().eventTypeId)
        val byEventType = request.captured.globalByEventTypeList.single()
        assertEquals("CustomerRegistered", byEventType.eventTypeId)
        assertEquals("customer-1", byEventType.entriesList.single().eventSourceId)
    }

    @Test
    fun `seed groups scoped entries by namespace and keeps unscoped entries global`() = runBlocking {
        val stub = mockk<EventSeedingGrpcKt.EventSeedingCoroutineStub>()
        val request = slot<Seeding.SeedEventsRequest>()
        coEvery { stub.seedEvents(capture(request), any()) } returns
            Seeding.CommandResult.newBuilder().setIsAuthorized(true).build()

        val service = EventSeedingService("my-store", "default", stub)
        service.seed(MultiNamespaceSeeder())

        val byNamespace = request.captured.namespacedEntriesList.associateBy { it.namespace }
        assertEquals(2, byNamespace.size)
        assertTrue(byNamespace.containsKey("tenant-a"))
        assertTrue(byNamespace.containsKey("tenant-b"))

        assertEquals("customer-1", request.captured.globalByEventSourceList.single().eventSourceId)
        assertEquals("customer-2", byNamespace.getValue("tenant-a").byEventSourceList.single().eventSourceId)
        assertEquals("customer-3", byNamespace.getValue("tenant-b").byEventSourceList.single().eventSourceId)
    }
}
