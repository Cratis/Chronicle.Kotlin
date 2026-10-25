// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import io.cratis.chronicle.json.chronicleGson
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.jackson.ConceptKeyFormat
import io.cratis.fundamentals.jackson.ConceptsModule
import io.cratis.fundamentals.jackson.EnumModule
import io.cratis.fundamentals.jackson.TemporalModule
import io.cratis.fundamentals.jackson.TimeSpanModule
import java.time.LocalTime
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule

val fundamentalsMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModules(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), TemporalModule(), EnumModule(), TimeSpanModule())
    .build()

val gsonParityMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModules(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), TemporalModule(), EnumModule(), TimeSpanModule())
    // Removes extra nullable members on event, geospatial and nulls fixtures.
    .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS) }
    // Restores private Java fields; disables getters to keep Gson's field-based contract.
    .changeDefaultVisibility { it.withFieldVisibility(Visibility.ANY).withGetterVisibility(Visibility.NONE).withIsGetterVisibility(Visibility.NONE) }
    // Removes the Java POJO's [count, name] alphabetical order after fields become visible.
    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    // Preserves incoming +02:00 offsets instead of converting offset/zoned values to UTC.
    .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
    .addModule(gsonWireModule())
    .build()

val comparisonMappers: Map<String, JsonMapper> = linkedMapOf("fundamentals" to fundamentalsMapper, "gsonParity" to gsonParityMapper)

private fun gsonWireModule(): SimpleModule = SimpleModule("GsonComparisonWire").apply {
    // Restores GeoJson envelopes/coordinate arrays using the existing Gson GeoJson adapters.
    bridge(Point::class.java)
    bridge(LineString::class.java)
    bridge(Polygon::class.java)
    // Removes seven-digit padding/truncation; preserves all nine non-zero nanosecond digits.
    bridge(LocalTime::class.java)
    // Neither PLAIN nor EMBEDDED_JSON matches Gson: default Gson map keys use toString().
    addKeySerializer(ConceptAs::class.java, object : ValueSerializer<ConceptAs<*>>() {
        override fun serialize(value: ConceptAs<*>, generator: JsonGenerator, context: SerializationContext) {
            generator.writeName(value.toString())
        }
    })
}

private fun <T : Any> SimpleModule.bridge(type: Class<T>) {
    addSerializer(type, object : ValueSerializer<T>() {
        override fun serialize(value: T, generator: JsonGenerator, context: SerializationContext) {
            generator.writeRawValue(chronicleGson.toJson(value))
        }
    })
    addDeserializer(type, object : ValueDeserializer<T>() {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T =
            chronicleGson.fromJson(context.readTree(parser).toString(), type)
    })
}
