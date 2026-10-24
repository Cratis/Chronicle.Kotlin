// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private enum class DisplayStatus {
    Dispatched {
        override fun toString(): String = "sent"
    },
    Scheduled {
        override fun toString(): String = " in transit "
    }
}

private enum class SerializedStatus {
    @SerializedName(value = "1", alternate = ["999"])
    Pending,
    @SerializedName(value = "sent-status", alternate = ["shipped-status", "legacy-sent", " sent before "])
    Dispatched
}

private data class SerializedDeliveryState(val status: SerializedStatus)

private enum class TextualStatus {
    @SerializedName(value = "Pending", alternate = ["Awaiting"])
    Dispatched,
    Pending,
    Awaiting;

    override fun toString(): String = when (this) {
        Dispatched -> "ready"
        Pending -> "Awaiting"
        Awaiting -> "sent"
    }
}

private enum class NameAndDisplayStatus {
    Pending, Dispatched;

    override fun toString(): String = "Dispatched"
}

private enum class ExactTextualStatus {
    @SerializedName(value = "READY", alternate = ["PENDING"])
    Serialized,
    Display,
    Pending;

    override fun toString(): String = if (this == Display) "ready" else name
}

class EnumTextualCompatibilityTests {
    @Test
    fun `a map key with an overridden toString round trips`() {
        val type = object : TypeToken<Map<DisplayStatus, DisplayStatus>>() {}.type
        val statuses = mapOf(
            DisplayStatus.Dispatched to DisplayStatus.Dispatched,
            DisplayStatus.Scheduled to DisplayStatus.Scheduled
        )
        val json = chronicleGson.toJson(statuses, type)

        assertEquals("""{"sent":0," in transit ":1}""", json)
        assertEquals(statuses, chronicleGson.fromJson<Map<DisplayStatus, DisplayStatus>>(json, type))
    }

    @ParameterizedTest
    @ValueSource(strings = ["sent", "SENT", "sEnT"])
    fun `overridden toString values are read case insensitively`(value: String) {
        assertEquals(DisplayStatus.Dispatched, chronicleGson.fromJson("\"$value\"", DisplayStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = [" in transit ", " IN TRANSIT "])
    fun `toString values retain their whitespace when read`(value: String) {
        assertEquals(DisplayStatus.Scheduled, chronicleGson.fromJson("\"$value\"", DisplayStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["sent-status", "shipped-status", "legacy-sent", "SENT-STATUS", "SHIPPED-STATUS", "LEGACY-SENT", " sent before ", " SENT BEFORE "])
    fun `stored serialized names and alternates remain readable`(value: String) {
        val json = """{"status":"$value"}"""
        assertEquals(SerializedDeliveryState(SerializedStatus.Dispatched), chronicleGson.fromJson(json, SerializedDeliveryState::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "\"1\""])
    fun `defined ordinals take precedence over numeric serialized names`(json: String) {
        assertEquals(SerializedStatus.Dispatched, chronicleGson.fromJson(json, SerializedStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["999", "\"999\""])
    fun `numeric serialized alternates do not allow undefined ordinals`(json: String) {
        assertThrows(JsonSyntaxException::class.java) {
            chronicleGson.fromJson(json, SerializedStatus::class.java)
        }
    }

    @Test
    fun `annotated constants still write ordinals and read their constant names`() {
        assertEquals("1", chronicleGson.toJson(SerializedStatus.Dispatched))
        assertEquals(SerializedStatus.Dispatched, chronicleGson.fromJson("\"Dispatched\"", SerializedStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Pending", "pending", "Awaiting", "awaiting"])
    fun `serialized names and alternates take precedence over names and toString values`(value: String) {
        assertEquals(TextualStatus.Dispatched, chronicleGson.fromJson("\"$value\"", TextualStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Dispatched", "dispatched"])
    fun `constant names take precedence over toString values`(value: String) {
        assertEquals(NameAndDisplayStatus.Dispatched, chronicleGson.fromJson("\"$value\"", NameAndDisplayStatus::class.java))
    }

    @ParameterizedTest
    @ValueSource(strings = ["READY", "ready"])
    fun `exact toString matches take precedence over case insensitive serialized names`(value: String) {
        val expected = if (value == "ready") ExactTextualStatus.Display else ExactTextualStatus.Serialized
        assertEquals(expected, chronicleGson.fromJson("\"$value\"", ExactTextualStatus::class.java))
    }

    @Test
    fun `an exact constant name takes precedence over a case insensitive serialized name`() {
        assertEquals(ExactTextualStatus.Pending, chronicleGson.fromJson("\"Pending\"", ExactTextualStatus::class.java))
    }
}
