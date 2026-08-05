// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.EventStoreSubscriptionsGrpcKt
import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.events.IEventTypesService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class OrderPlaced(val orderId: String)

class EventStoreSubscriptionsServiceTests {

    @Test
    fun `subscribe with no withEventType calls falls back to every registered event type`() = runBlocking {
        val stub = mockk<EventStoreSubscriptionsGrpcKt.EventStoreSubscriptionsCoroutineStub>()
        val request = slot<ObservationEventstoresubscriptions.AddEventStoreSubscriptions>()
        coEvery { stub.add(capture(request), any()) } returns Empty.getDefaultInstance()

        val eventTypes = mockk<IEventTypesService>()
        val registered = listOf(
            EventTypeDescriptor(EventTypeId("OrderPlaced"), EventTypeGeneration(1), false),
            EventTypeDescriptor(EventTypeId("OrderShipped"), EventTypeGeneration(1), false)
        )
        every { eventTypes.getRegisteredEventTypes() } returns registered

        val service = EventStoreSubscriptionsService("my-store", stub, eventTypes)
        service.subscribe("sub-1", "source-store") { }

        val sentEventTypes = request.captured.subscriptionsList.single().eventTypesList
        assertEquals(2, sentEventTypes.size)
        assertTrue(sentEventTypes.any { it.id == "OrderPlaced" })
        assertTrue(sentEventTypes.any { it.id == "OrderShipped" })
    }

    @Test
    fun `subscribe with an explicit withEventType call only sends the specified event types`() = runBlocking {
        val stub = mockk<EventStoreSubscriptionsGrpcKt.EventStoreSubscriptionsCoroutineStub>()
        val request = slot<ObservationEventstoresubscriptions.AddEventStoreSubscriptions>()
        coEvery { stub.add(capture(request), any()) } returns Empty.getDefaultInstance()

        val eventTypes = mockk<IEventTypesService>()
        every { eventTypes.getRegisteredEventTypes() } returns listOf(
            EventTypeDescriptor(EventTypeId("OrderPlaced"), EventTypeGeneration(1), false),
            EventTypeDescriptor(EventTypeId("OrderShipped"), EventTypeGeneration(1), false)
        )

        val service = EventStoreSubscriptionsService("my-store", stub, eventTypes)
        service.subscribe("sub-1", "source-store") { it.withEventType(OrderPlaced::class) }

        val sentEventTypes = request.captured.subscriptionsList.single().eventTypesList
        assertEquals(1, sentEventTypes.size)
        assertEquals("OrderPlaced", sentEventTypes.single().id)
    }
}
