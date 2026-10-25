// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.api.function.Executable

@EventType("JsonEngineComparisonEvent")
@JsonAdapter(RawJsonAdapter::class)
class RawEventContent(val json: String)

class RawJsonAdapter : TypeAdapter<RawEventContent>() {
    override fun write(out: JsonWriter, value: RawEventContent) {
        out.jsonValue(value.json)
    }

    override fun read(reader: JsonReader): RawEventContent = throw UnsupportedOperationException("Write-only comparison wrapper")
}

/** CI's existing kernel lane owns this evidence; local runs must skip, never start a kernel. */
class JsonEngineKernelComparisonTests {
    @Test
    fun `each engine appends against the real schema with a negative control`(reporter: TestReporter) = runBlocking {
        val connectionString = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI for the kernel roundtrip test")
        }
        assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run this test against a Chronicle kernel")
        val options = ChronicleOptions.fromConnectionString(connectionString!!).copy(
            autoDiscoverAndRegister = true,
            artifacts = KnownClientArtifacts(JsonEngineComparisonEvent::class)
        )
        ChronicleClient(options).use { client ->
            val store = client.getEventStore("JsonComparison${UUID.randomUUID().toString().replace("-", "").take(12)}")
            store.awaitRegistration()
            val event = ComparisonFixtures.event
            val contents = linkedMapOf<String, Any>("gson" to event)
            comparisonMappers.forEach { (name, mapper) -> contents[name] = RawEventContent(mapper.writeValueAsString(event)) }
            contents["negative"] = RawEventContent(chronicleGson.toJson(event).replace("\"count\":3", "\"count\":\"not-a-number\""))
            // Collect every append before asserting, so CI records all engines even on a rejection.
            val results = contents.mapValues { (_, content) -> store.eventLog.append(UUID.randomUUID().toString(), content) }
            results.forEach { (name, result) -> reporter.publishEntry(name, "isSuccess=${result.isSuccess}; errors=${result.errors}") }
            // Fundamentals is an observation pending the first CI run, not a guessed outcome.
            val expected: Map<String, Boolean?> = mapOf("gson" to true, "fundamentals" to null, "gsonParity" to true, "negative" to false)
            val checks = expected.mapNotNull { (name, success) ->
                success?.let { Executable { assertEquals(it, results.getValue(name).isSuccess, "$name: ${results.getValue(name).errors}") } }
            }.toMutableList()
            val stored = store.eventLog.getFromSequenceNumber(EventSequenceNumber.first)
            checks.add(Executable { assertEquals(results.values.count { it.isSuccess }, stored.size, "every successful append is stored") })
            stored.forEach { item ->
                checks.add(Executable { assertEquals(event, chronicleGson.fromJson(item.content, JsonEngineComparisonEvent::class.java), item.content) })
            }
            assertAll(checks)
        }
    }
}
