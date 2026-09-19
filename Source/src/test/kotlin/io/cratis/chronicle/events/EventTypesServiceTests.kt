// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.EventTypes.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.EventTypes.Eventtypes
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.ChronicleCommandRejected
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class ProductRegistered(val name: String, val price: Double)

class EventTypesServiceTests {

    @Test
    fun `register sends a real schema reflecting the event class's properties, not an empty object`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        val request = slot<Eventtypes.RegisterEventTypesRequest>()
        coEvery { stub.registerEventTypes(capture(request), any()) } returns
            Eventtypes.CommandResult.newBuilder().setIsAuthorized(true).build()

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
    fun `register throws when the kernel reports the registration failed`() = runBlocking {
        // Reproduces #86 - the kernel's CommandResult was discarded, so a rejected registration
        // looked identical to a successful one and the event type was silently never stored.
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.registerEventTypes(any(), any()) } returns
            Eventtypes.CommandResult.newBuilder()
                .setIsAuthorized(true)
                .addExceptionMessages("Event type 'ProductRegistered' with generation '1' is missing from the event store")
                .build()

        val service = EventTypesService("my-event-store", stub)
        assertThrows(ChronicleCommandRejected::class.java) {
            runBlocking { service.register(ProductRegistered::class) }
        }
    }

    @Test
    fun `register throws when the kernel reports the caller is not authorized`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.registerEventTypes(any(), any()) } returns
            Eventtypes.CommandResult.newBuilder()
                .setIsAuthorized(false)
                .setAuthorizationFailureReason("not allowed")
                .build()

        val service = EventTypesService("my-event-store", stub)
        assertThrows(ChronicleCommandRejected::class.java) {
            runBlocking { service.register(ProductRegistered::class) }
        }
    }

    @Test
    fun `registerSingle throws when the kernel reports the registration failed`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.registerSingleEventType(any(), any()) } returns
            Eventtypes.CommandResult.newBuilder()
                .setIsAuthorized(true)
                .addExceptionMessages("rejected")
                .build()

        val service = EventTypesService("my-event-store", stub)
        assertThrows(ChronicleCommandRejected::class.java) {
            runBlocking { service.registerSingle(ProductRegistered::class) }
        }
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
        coEvery { stub.registerEventTypes(any(), any()) } returns
            Eventtypes.CommandResult.newBuilder().setIsAuthorized(true).build()

        val service = EventTypesService("my-event-store", stub)
        service.register(ProductRegistered::class)

        val registered = service.getRegisteredEventTypes()
        assertEquals(1, registered.size)
        assertEquals("ProductRegistered", registered.single().id.value)
    }

    @Test
    fun `getRegisteredEventTypes reflects a class registered through registerSingle`() = runBlocking {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        coEvery { stub.registerSingleEventType(any(), any()) } returns
            Eventtypes.CommandResult.newBuilder().setIsAuthorized(true).build()

        val service = EventTypesService("my-event-store", stub)
        service.registerSingle(ProductRegistered::class)

        assertEquals(1, service.getRegisteredEventTypes().size)
    }
}
