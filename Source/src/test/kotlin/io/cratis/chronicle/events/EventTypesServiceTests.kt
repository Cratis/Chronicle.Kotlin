// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.Events.Events
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.protobuf.Empty
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.reflect.KClass
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@EventType(id = "EmployeeHired")
private data class EmployeeHired(val firstName: String, val lastName: String, val title: String)

private class NotAnEventType(val name: String)

class EventTypesServiceTests {

    /**
     * Registers the given classes against a mocked kernel stub and returns the request that was sent,
     * or null when nothing was sent.
     */
    private suspend fun register(vararg classes: KClass<*>): Events.RegisterEventTypesRequest? {
        val stub = mockk<EventTypesGrpcKt.EventTypesCoroutineStub>()
        val request = slot<Events.RegisterEventTypesRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        EventTypesService("MyStore", stub).register(*classes)

        return if (request.isCaptured) request.captured else null
    }

    private fun Events.EventTypeRegistration.schemaAsJson(): JsonObject =
        Gson().fromJson(schema, JsonObject::class.java)

    @Test
    fun `registers the event type with a schema derived from the class`() = runTest {
        val schema = register(EmployeeHired::class)!!.getTypes(0).schemaAsJson()

        assertEquals("object", schema.get("type").asString)
        assertEquals(
            setOf("firstName", "lastName", "title"),
            schema.getAsJsonObject("properties").keySet()
        )
    }

    @Test
    fun `registers the event type with property types from the class`() = runTest {
        val properties = register(EmployeeHired::class)!!.getTypes(0).schemaAsJson()
            .getAsJsonObject("properties")

        assertEquals("string", properties.getAsJsonObject("firstName").get("type").asString)
    }

    @Test
    fun `does not register the event type with an empty schema`() = runTest {
        assertNotEquals("{}", register(EmployeeHired::class)!!.getTypes(0).schema)
    }

    @Test
    fun `registers the event type with the id from the annotation`() = runTest {
        assertEquals("EmployeeHired", register(EmployeeHired::class)!!.getTypes(0).type.id)
    }

    @Test
    fun `ignores classes without the event type annotation`() = runTest {
        assertNull(register(NotAnEventType::class))
    }
}
