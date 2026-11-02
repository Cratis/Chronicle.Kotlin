// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Base64

/**
 * Writes bytes as standard padded base64, matching .NET byte arrays and the kernel's byte-array format.
 * Reads also accept URL-safe or unpadded base64 and legacy numeric arrays (signed or unsigned bytes).
 * Registered with [nullSafe] on [chronicleGson].
 */
internal class ByteArrayTypeAdapter : TypeAdapter<ByteArray>() {
    override fun write(out: JsonWriter, value: ByteArray) {
        out.value(Base64.getEncoder().encodeToString(value))
    }

    override fun read(reader: JsonReader): ByteArray {
        try {
            return when (reader.peek()) {
                JsonToken.STRING -> Base64.getDecoder().decode(reader.nextString().replace('-', '+').replace('_', '/'))
                JsonToken.BEGIN_ARRAY -> readLegacyArray(reader)
                else -> throw JsonParseException("Expected a base64 string or byte array at ${reader.path}")
            }
        } catch (exception: IllegalArgumentException) {
            throw JsonParseException("Invalid byte-array value at ${reader.path}", exception)
        } catch (exception: ArithmeticException) {
            throw JsonParseException("Expected an integer byte at ${reader.path}", exception)
        }
    }

    private fun readLegacyArray(reader: JsonReader): ByteArray {
        val bytes = mutableListOf<Byte>()
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.NUMBER) {
                throw JsonParseException("Expected an integer byte at ${reader.path}")
            }
            val value = reader.nextString().toBigDecimal().intValueExact()
            require(value in -128..255) { "Byte value is outside -128..255" }
            bytes.add(value.toByte())
        }
        reader.endArray()
        return bytes.toByteArray()
    }
}
