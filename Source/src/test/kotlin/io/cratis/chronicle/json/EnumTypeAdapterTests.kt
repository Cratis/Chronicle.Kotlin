// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private enum class DeliveryStatus { Pending, Dispatched }

private enum class StatusWithBody {
    Dispatched {
        override fun toString(): String = "sent"
    }
}

@EventType
private data class DeliveryStatusChanged(val status: DeliveryStatus)

private data class DeliveryState(val status: DeliveryStatus?)

class EnumTypeAdapterTests {
    @Test
    fun `an enum property of an event is written as its ordinal number`() {
        assertEquals("""{"status":1}""", chronicleGson.toJson(DeliveryStatusChanged(DeliveryStatus.Dispatched)))
    }

    @Test
    fun `an enum property of a read model is written as its ordinal number`() {
        assertEquals("""{"status":1}""", chronicleGson.toJson(DeliveryState(DeliveryStatus.Dispatched)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "1"])
    fun `defined numeric ordinals are read`(json: String) {
        assertEquals(DeliveryStatus.entries[json.toInt()], chronicleGson.fromJson(json, DeliveryStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "1"])
    fun `defined numeric strings are read`(ordinal: String) {
        assertEquals(DeliveryStatus.entries[ordinal.toInt()], chronicleGson.fromJson("\"$ordinal\"", DeliveryStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Dispatched", "dispatched", "DISPATCHED", "dIsPaTcHeD"])
    fun `constant names are read case insensitively`(name: String) {
        assertEquals(DeliveryStatus.Dispatched, chronicleGson.fromJson("\"$name\"", DeliveryStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "2", "999", "2147483648"])
    fun `undefined numeric ordinals fail with the enum type and value`(ordinal: String) {
        val error = assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson(ordinal, DeliveryStatus::class.java)
        }
        assertTrue(error.message!!.contains(DeliveryStatus::class.java.name), error.message)
        assertTrue(error.message!!.contains(ordinal), error.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "2", "999", "2147483648"])
    fun `undefined numeric strings fail with the enum type and value`(ordinal: String) {
        val error = assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson("\"$ordinal\"", DeliveryStatus::class.java)
        }
        assertTrue(error.message!!.contains(DeliveryStatus::class.java.name), error.message)
        assertTrue(error.message!!.contains(ordinal), error.message)
    }

    @Test
    fun `an unknown name fails with the enum type and value`() {
        val error = assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson("\"Missing\"", DeliveryStatus::class.java)
        }
        assertTrue(error.message!!.contains(DeliveryStatus::class.java.name), error.message)
        assertTrue(error.message!!.contains("Missing"), error.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.5", "1.0", "1e0", "\"1.5\""])
    fun `noninteger representations are rejected rather than truncated`(json: String) {
        assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson(json, DeliveryStatus::class.java)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["true", "[]", "{}"])
    fun `tokens other than numbers strings and null fail clearly`(json: String) {
        val error = assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson(json, DeliveryStatus::class.java)
        }
        assertTrue(error.message!!.contains(DeliveryStatus::class.java.name), error.message)
    }

    @Test
    fun `nullable enums stay null`() {
        assertEquals("null", chronicleGson.toJson(null, DeliveryStatus::class.java))
        assertNull(chronicleGson.fromJson("null", DeliveryStatus::class.java))
        val state = chronicleGson.fromJson("""{"status":null}""", DeliveryState::class.java)
        assertNull(state.status)
        assertEquals(DeliveryState(null), chronicleGson.fromJson(chronicleGson.toJson(state), DeliveryState::class.java))
    }

    @Test
    fun `event and read model properties read both ordinals and legacy names`() {
        for (value in listOf("1", "\"Dispatched\"")) {
            val json = """{"status":$value}"""
            assertEquals(DeliveryStatusChanged(DeliveryStatus.Dispatched), chronicleGson.fromJson(json, DeliveryStatusChanged::class.java))
            assertEquals(DeliveryState(DeliveryStatus.Dispatched), chronicleGson.fromJson(json, DeliveryState::class.java))
        }
    }

    @Test
    fun `enums inside collections are written as numbers and read from numbers or names`() {
        val type = object : TypeToken<List<DeliveryStatus>>() {}.type
        val statuses = listOf(DeliveryStatus.Pending, DeliveryStatus.Dispatched)
        assertEquals("[0,1]", chronicleGson.toJson(statuses, type))
        assertEquals(statuses, chronicleGson.fromJson<List<DeliveryStatus>>("[0,\"Dispatched\"]", type))
    }

    @Test
    fun `enum map keys remain names while enum map values are numbers`() {
        val type = object : TypeToken<Map<DeliveryStatus, DeliveryStatus>>() {}.type
        val statuses = mapOf(DeliveryStatus.Pending to DeliveryStatus.Dispatched)
        assertEquals("""{"Pending":1}""", chronicleGson.toJson(statuses, type))
        assertEquals(statuses, chronicleGson.fromJson<Map<DeliveryStatus, DeliveryStatus>>("""{"Pending":1}""", type))
    }

    @Test
    fun `a constant with a class body writes its ordinal even through runtime type lookup`() {
        assertEquals("0", chronicleGson.toJson(StatusWithBody.Dispatched))
        assertEquals(StatusWithBody.Dispatched, chronicleGson.fromJson("0", StatusWithBody::class.java))
    }
}
