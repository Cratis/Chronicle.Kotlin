// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParser
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventType
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

enum class KernelDeliveryStatus { Pending, Dispatched }

@EventType
data class EnumDeliveryRecorded(val status: KernelDeliveryStatus)

/**
 * Uses CI's Chronicle kernel to verify that enum content satisfies its registered integer schema.
 * Requires `CHRONICLE_INTEGRATION_CONNECTION_STRING`; skipped locally when it is absent.
 */
class EnumKernelRoundtripTests {
    @Test
    fun `an event with an enum property appends and reads back as an ordinal number`() = runBlocking {
        val connectionString = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI")
        } else {
            assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run against a kernel")
        }
        val options = ChronicleOptions.fromConnectionString(connectionString!!).copy(
            autoDiscoverAndRegister = true,
            artifacts = KnownClientArtifacts(EnumDeliveryRecorded::class)
        )
        ChronicleClient(options).use { client ->
            val store = client.getEventStore("EnumRoundtrip${UUID.randomUUID().toString().replace("-", "").take(12)}")
            store.awaitRegistration()
            val event = EnumDeliveryRecorded(KernelDeliveryStatus.Dispatched)
            val result = store.eventLog.append(UUID.randomUUID().toString(), event)
            assertTrue(result.isSuccess, result.errors.toString())

            val stored = store.eventLog.getFromSequenceNumber(EventSequenceNumber.first).single()
            val status = JsonParser.parseString(stored.content).asJsonObject.getAsJsonPrimitive("status")
            assertTrue(status.isNumber, stored.content)
            assertEquals(1, status.asInt)
            assertEquals(event, chronicleGson.fromJson(stored.content, EnumDeliveryRecorded::class.java))
        }
    }
}
