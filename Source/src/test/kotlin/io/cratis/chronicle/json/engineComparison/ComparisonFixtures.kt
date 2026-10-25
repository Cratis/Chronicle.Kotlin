// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.LinearRing
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import io.cratis.chronicle.json.chronicleGson
import io.cratis.fundamentals.concepts.ConceptAs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

data class StringConcept(val value: String) : ConceptAs<String> {
    override fun value(): String = value
}

data class ComparisonSourceId(val value: String) : EventSourceId {
    override fun value(): String = value
}

data class UuidConcept(val value: UUID) : ConceptAs<UUID> {
    override fun value(): UUID = value
}

data class IntConcept(private val number: Int) : ConceptAs<Int> {
    override fun value(): Int = number
}

enum class ComparisonStatus {
    Pending,
    Dispatched,
    @SerializedName("delivered-wire") Delivered,
    WithBody { override fun toString(): String = "custom-body" }
}

data class ComparisonNested(val label: String, val quantity: Int)

@EventType("JsonEngineComparisonEvent")
data class JsonEngineComparisonEvent(
    val name: String,
    val count: Int,
    val total: Long,
    val price: Double,
    val isActive: Boolean,
    val id: UUID,
    val tags: List<String>,
    val counts: Map<String, Int>,
    val nested: ComparisonNested,
    val optional: String?,
    val concept: StringConcept,
    val status: ComparisonStatus,
    val at: Instant,
    val location: Point
)

/** A separate map-free, non-null fixture for kernel acceptance, not a replacement for the golden event. */
@EventType("JsonEngineComparisonSimpleEvent")
data class JsonEngineComparisonSimpleEvent(
    val name: String,
    val count: Int,
    val tags: List<String>,
    val nested: ComparisonNested,
    val concept: StringConcept,
    val status: ComparisonStatus,
    val at: Instant
)

data class ComparisonReadModel(val name: String, val groups: List<List<ComparisonNested>>)
data class ComparisonConcepts(
    val text: StringConcept,
    val uuid: UuidConcept,
    val number: IntConcept,
    val source: ComparisonSourceId,
    val keys: Map<StringConcept, String>
)
data class ComparisonTemporal(
    val instant: Instant,
    val offset: OffsetDateTime,
    val zoned: ZonedDateTime,
    val dateTime: LocalDateTime,
    val date: LocalDate,
    val time: LocalTime,
    val wholeTime: LocalTime
)
data class ComparisonGeospatial(val point: Point, val line: LineString, val polygon: Polygon, val absent: Point?)
data class ComparisonEnums(
    val status: ComparisonStatus,
    val statuses: List<ComparisonStatus>,
    val keys: Map<ComparisonStatus, String>,
    val renamed: ComparisonStatus,
    val body: ComparisonStatus
)
data class ComparisonNulls(val text: String?, val point: Point?, val items: List<String>, val keys: Map<String, Int>)

data class HistoricalRequired(val name: String, val count: Int)
data class HistoricalDefault(val name: String, val count: Int = 7)
data class HistoricalPrivate private constructor(val name: String) {
    companion object {
        fun sample(): HistoricalPrivate = HistoricalPrivate("historical")
    }
}

data class ComparisonFixture(val name: String, val sample: Any) {
    val type: Class<*> get() = sample.javaClass
}

object ComparisonFixtures {
    private val id = UUID.fromString("48e95d01-b98f-4280-8731-54bb12c3abc4")
    private val instant = Instant.parse("2024-05-01T10:00:00.123456789Z")
    val event = JsonEngineComparisonEvent(
        "delivery", 3, 9007199254740991L, 12.5, true, id,
        listOf("first", "second"), linkedMapOf("z" to 2, "a" to 1), ComparisonNested("parcel", 2),
        null, StringConcept("order-1"), ComparisonStatus.Dispatched, instant, Point(10.5, 59.9)
    )
    val simpleEvent = JsonEngineComparisonSimpleEvent(
        "delivery", 3, listOf("first", "second"), ComparisonNested("parcel", 2),
        StringConcept("order-1"), ComparisonStatus.Dispatched, instant
    )
    val all = listOf(
        ComparisonFixture("event", event),
        ComparisonFixture("read-model", ComparisonReadModel("nested", listOf(listOf(ComparisonNested("one", 1), ComparisonNested("two", 2))))),
        ComparisonFixture("concepts", ComparisonConcepts(StringConcept("book"), UuidConcept(id), IntConcept(42), ComparisonSourceId("source-1"), linkedMapOf(StringConcept("key") to "value"))),
        ComparisonFixture("temporal", ComparisonTemporal(instant, OffsetDateTime.parse("2024-05-01T12:00:00.123456789+02:00"), ZonedDateTime.parse("2024-05-01T12:00:00.123456789+02:00[Europe/Oslo]"), LocalDateTime.parse("2024-05-01T10:00:00.123456789"), LocalDate.parse("2024-05-01"), LocalTime.parse("10:00:00.123456789"), LocalTime.of(10, 0))),
        ComparisonFixture("geospatial", ComparisonGeospatial(Point(10.5, 59.9), LineString(Point(1.0, 2.0), Point(3.0, 4.0)), Polygon(LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))), null)),
        ComparisonFixture("enums", ComparisonEnums(ComparisonStatus.Dispatched, listOf(ComparisonStatus.Pending, ComparisonStatus.Delivered), linkedMapOf(ComparisonStatus.Delivered to "renamed", ComparisonStatus.WithBody to "body"), ComparisonStatus.Delivered, ComparisonStatus.WithBody)),
        ComparisonFixture("nulls", ComparisonNulls(null, null, emptyList(), emptyMap())),
        ComparisonFixture("java-pojo", ComparisonPojo()),
        ComparisonFixture("java-record", ComparisonRecord("record", ComparisonStatus.Dispatched, instant))
    )

    fun invalidCountJson(event: Any): String = JsonParser.parseString(chronicleGson.toJson(event)).asJsonObject.apply {
        addProperty("count", "not-a-number")
    }.toString()

    fun resource(folder: String, name: String): String = requireNotNull(
        ComparisonFixtures::class.java.getResource("/json-engine-comparison/$folder/$name.json")
    ).readText().trim()
}
