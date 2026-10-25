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
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
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

@EventType("JsonEngineComparisonSimpleEvent")
@JsonAdapter(RawSimpleJsonAdapter::class)
class RawSimpleEventContent(val json: String)

class RawSimpleJsonAdapter : TypeAdapter<RawSimpleEventContent>() {
    override fun write(out: JsonWriter, value: RawSimpleEventContent) {
        out.jsonValue(value.json)
    }

    override fun read(reader: JsonReader): RawSimpleEventContent = throw UnsupportedOperationException("Write-only comparison wrapper")
}

/** Schema rejections arrive as constraint violations, not append errors. */
internal fun AppendResult.hasSchemaValidationViolation(vararg evidence: String): Boolean = constraintViolations.any { violation ->
    val description = "${violation.message} ${violation.details}"
    violation.constraintId == "SchemaValidation" && evidence.all { description.contains(it) }
}

/** CI's existing kernel lane owns this evidence; local runs must skip, never start a kernel. */
class JsonEngineKernelComparisonTests {
    @Test
    fun `the original complex event is rejected for the existing schema limitations`(reporter: TestReporter) = runBlocking {
        ChronicleClient(optionsFor(JsonEngineComparisonEvent::class)).use { client ->
            val store = client.getEventStore("JsonComparison${UUID.randomUUID().toString().replace("-", "").take(12)}")
            store.awaitRegistration()
            val event = ComparisonFixtures.event
            val contents = linkedMapOf<String, Any>("gson" to event)
            comparisonMappers.forEach { (name, mapper) -> contents[name] = RawEventContent(mapper.writeValueAsString(event)) }
            contents["negative"] = RawEventContent(ComparisonFixtures.invalidCountJson(event))
            // Collect every append before asserting, so CI records all engines even on a rejection.
            val results = contents.mapValues { (_, content) -> store.eventLog.append(UUID.randomUUID().toString(), content) }
            results.forEach { (name, result) -> observe(reporter, "complex.$name", result.toString()) }
            val checks = results.map { (name, result) -> Executable {
                assertEquals(false, result.isSuccess, "$name: $result")
                assertTrue(result.hasSchemaValidationViolation("counts", "string", "object"), "$name: $result")
            } }.toMutableList()
            // These additional violations were observed in CI; they do not establish broader null behavior.
            checks.add(Executable { assertTrue(results.getValue("fundamentals").hasSchemaValidationViolation("optional", "string", "null"), results.getValue("fundamentals").toString()) })
            checks.add(Executable { assertTrue(results.getValue("negative").hasSchemaValidationViolation("count", "integer", "string"), results.getValue("negative").toString()) })
            val stored = store.eventLog.getFromSequenceNumber(EventSequenceNumber.first)
            observe(reporter, "complex.stored", stored.size.toString())
            checks.add(Executable { assertEquals(0, stored.size, "all original complex-event appends are rejected") })
            assertAll(checks)
        }
    }

    @Test
    fun `a separate map-free event appends and reads back with an isolated count negative control`(reporter: TestReporter) = runBlocking {
        ChronicleClient(optionsFor(JsonEngineComparisonSimpleEvent::class)).use { client ->
            val store = client.getEventStore("JsonSimple${UUID.randomUUID().toString().replace("-", "").take(12)}")
            store.awaitRegistration()
            val event = ComparisonFixtures.simpleEvent
            // CI run 40148807431: the kernel truncates Instant values to 100 ns for every engine.
            // Keep the nine-digit input unchanged so the lost 89 ns remains visible evidence.
            val expectedReadBack = event.copy(at = Instant.parse("2024-05-01T10:00:00.123456700Z"))
            val contents = linkedMapOf<String, Any>("gson" to event)
            comparisonMappers.forEach { (name, mapper) -> contents[name] = RawSimpleEventContent(mapper.writeValueAsString(event)) }
            contents["negative"] = RawSimpleEventContent(ComparisonFixtures.invalidCountJson(event))
            val results = contents.mapValues { (_, content) -> store.eventLog.append(UUID.randomUUID().toString(), content) }
            results.forEach { (name, result) -> observe(reporter, "simple.$name", result.toString()) }
            // All three engines were accepted in CI; the isolated count-string negative was rejected.
            val expected = mapOf("gson" to true, "fundamentals" to true, "gsonParity" to true, "negative" to false)
            val checks = expected.map { (name, success) ->
                Executable { assertEquals(success, results.getValue(name).isSuccess, "$name: ${results.getValue(name)}") }
            }.toMutableList()
            checks.add(Executable { assertTrue(results.getValue("negative").hasSchemaValidationViolation("count", "integer", "string"), results.getValue("negative").toString()) })
            val stored = store.eventLog.getFromSequenceNumber(EventSequenceNumber.first)
            checks.add(Executable { assertEquals(results.values.count { it.isSuccess }, stored.size, "every successful append is stored") })
            // Stored events keep append order, so they pair with the successful appends in insertion order.
            val storedNames = results.filterValues { it.isSuccess }.keys.toList()
            stored.zip(storedNames).forEach { (item, name) ->
                val readBack = runCatching { chronicleGson.fromJson(item.content, JsonEngineComparisonSimpleEvent::class.java) }
                observe(reporter, "simple.$name.readBack", readBack.fold({
                    "equalToInput=${it == event}, equalToKernelExpected=${it == expectedReadBack}, inputAt=${event.at}, storedAt=${it.at}, kernelPrecision=100ns"
                }, { "failed=${it.message}" }))
                checks.add(Executable { assertEquals(expectedReadBack, readBack.getOrThrow(), "kernel 100ns truncation: ${item.content}") })
            }
            assertAll(checks)
        }
    }

    private fun optionsFor(eventType: KClass<*>): ChronicleOptions {
        val connectionString = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI for the kernel roundtrip test")
        }
        assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run this test against a Chronicle kernel")
        return ChronicleOptions.fromConnectionString(connectionString!!).copy(
            autoDiscoverAndRegister = true,
            // Raw wrappers never register schemas; each scenario uses its actual event type only.
            artifacts = KnownClientArtifacts(eventType)
        )
    }

    private fun observe(reporter: TestReporter, key: String, value: String) {
        reporter.publishEntry(key, value)
        println("[json-engine-kernel] $key: $value")
    }
}
