// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Writes enums as ordinal numbers, matching their generated integer schemas.
 *
 * Reads defined ordinals from numbers or numeric strings. Text accepts serialized names and
 * alternates, constant names, and `toString()` values, preferring exact matches before ignoring case.
 * Unlike the .NET converter's string parsing, undefined numeric strings are rejected: JVM enums
 * cannot represent an undefined ordinal. Gson's default map handling keeps keys textual; this
 * adapter changes enum values, not the existing map-key representation.
 */
internal class EnumTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!Enum::class.java.isAssignableFrom(rawType) || rawType == Enum::class.java) return null
        // A constant with a class body has its own runtime subclass of the declaring enum.
        val enumType = if (rawType.isEnum) rawType else rawType.superclass
        return EnumTypeAdapter<T>(enumType).nullSafe()
    }
}

private class EnumTypeAdapter<T>(private val enumType: Class<*>) : TypeAdapter<T>() {
    private val constants = enumType.enumConstants.map { it as Enum<*> }
    // Resolve every exact match before considering case-insensitive matches, in this priority order.
    private val nameLookups = listOf(
        serializedNames(),
        constants.associateBy { it.name },
        constants.associateBy { it.toString() }
    )

    private fun serializedNames(): Map<String, Enum<*>> = buildMap {
        constants.forEach { constant ->
            val annotation = enumType.getField(constant.name).getAnnotation(SerializedName::class.java)
                ?: return@forEach
            put(annotation.value, constant)
            annotation.alternate.forEach { put(it, constant) }
        }
    }

    private fun readName(value: String): Enum<*>? =
        nameLookups.firstNotNullOfOrNull { it[value] }
            ?: nameLookups.firstNotNullOfOrNull { lookup ->
                lookup.entries.firstOrNull { it.key.equals(value, ignoreCase = true) }?.value
            }

    private fun readConstant(token: JsonToken, value: String): Enum<*>? {
        val normalized = value.trim()
        val ordinal = normalized.toIntOrNull()
        if (ordinal != null) return constants.getOrNull(ordinal)
        if (token != JsonToken.STRING) return null
        return readName(value) ?: readName(normalized)
    }

    override fun write(out: JsonWriter, value: T) {
        out.value((value as Enum<*>).ordinal)
    }

    override fun read(reader: JsonReader): T {
        val token = reader.peek()
        if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
            throw JsonSyntaxException("Expected an ordinal or name for enum '${enumType.name}', got $token at ${reader.path}")
        }
        val value = reader.nextString()
        val constant = readConstant(token, value)
        if (constant == null) {
            throw JsonSyntaxException("Unable to convert '$value' to enum '${enumType.name}' at ${reader.previousPath}: value is not defined")
        }
        @Suppress("UNCHECKED_CAST")
        return constant as T
    }
}
