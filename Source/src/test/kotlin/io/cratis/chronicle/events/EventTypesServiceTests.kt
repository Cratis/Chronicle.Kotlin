// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.Events.Events
import com.google.gson.Gson
import com.google.protobuf.Empty
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class ProductRegistered(val name: String, val price: Double)

class EventTypesServiceTests {

    @Test
    fun `register sends a real schema reflecting the event class's properties, not an empty object`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        val request = slot<Events.RegisterEventTypesRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = EventTypesService("my-event-store", stub)
        service.register(ProductRegistered::class)

        val schema = request.captured.typesList.single().schema
        assertNotEquals("{}", schema)

        @Suppress("UNCHECKED_CAST")
        val parsed = Gson().fromJson(schema, Map::class.java) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val properties = parsed["properties"] as Map<String, Any?>
        assertTrue(properties.containsKey("name"))
        assertTrue(properties.containsKey("price"))
        assertEquals("string", (properties["name"] as Map<*, *>)["type"])
        assertEquals("number", (properties["price"] as Map<*, *>)["type"])
    }

    @Test
    fun `getRegisteredEventTypes is empty before anything has been registered`() {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        val service = EventTypesService("my-event-store", stub)

        assertTrue(service.getRegisteredEventTypes().isEmpty())
    }

    @Test
    fun `getRegisteredEventTypes reflects every class registered through register`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.register(any(), any()) } returns Empty.getDefaultInstance()

        val service = EventTypesService("my-event-store", stub)
        service.register(ProductRegistered::class)

        val registered = service.getRegisteredEventTypes()
        assertEquals(1, registered.size)
        assertEquals("ProductRegistered", registered.single().id.value)
    }

    @Test
    fun `getRegisteredEventTypes reflects a class registered through registerSingle`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.registerSingle(any(), any()) } returns Empty.getDefaultInstance()

        val service = EventTypesService("my-event-store", stub)
        service.registerSingle(ProductRegistered::class)

        assertEquals(1, service.getRegisteredEventTypes().size)
    }
}
