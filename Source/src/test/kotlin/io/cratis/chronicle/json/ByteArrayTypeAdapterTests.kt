// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ByteArrayTypeAdapterTests {
    private data class Content(val duration: Duration? = null, val bytes: ByteArray? = null)

    private fun read(json: String): ByteArray = chronicleGson.fromJson(json, ByteArray::class.java)

    @Test
    fun `bytes serialize as base64 and round-trip`() {
        val bytes = byteArrayOf(1, 2, 3)
        assertEquals("\"AQID\"", chronicleGson.toJson(bytes))
        assertArrayEquals(bytes, read("\"AQID\""))
    }

    @Test
    fun `empty bytes serialize as an empty string and round-trip`() {
        assertEquals("\"\"", chronicleGson.toJson(byteArrayOf()))
        assertArrayEquals(byteArrayOf(), read("\"\""))
    }

    @Test
    fun `all byte values round-trip using standard padded base64`() {
        val bytes = ByteArray(256) { it.toByte() }
        assertArrayEquals(bytes, read(chronicleGson.toJson(bytes)))
        // Gson escapes equals signs in JSON strings by default; the decoded string still has padding.
        assertEquals("/w==", com.google.gson.JsonParser.parseString(chronicleGson.toJson(byteArrayOf(-1))).asString)
        assertArrayEquals(byteArrayOf(-1), read("\"/w==\""))
    }

    @Test
    fun `URL-safe base64 and missing padding are accepted`() {
        assertArrayEquals(byteArrayOf(-5, -1), read("\"-_8=\""))
        assertArrayEquals(byteArrayOf(-5, -1), read("\"-_8\""))
        assertArrayEquals(byteArrayOf(1), read("\"AQ\""))
    }

    @Test
    fun `legacy numeric arrays are accepted`() {
        assertArrayEquals(byteArrayOf(1, 2, 3), read("[1,2,3]"))
        assertArrayEquals(byteArrayOf(-128, -1, 0, 127), read("[-128,-1,0,127]"))
        assertArrayEquals(byteArrayOf(-128, -1), read("[128,255]"))
        assertArrayEquals(byteArrayOf(), read("[]"))
    }

    @Test
    fun `invalid base64 is rejected`() {
        listOf("\"!\"", "\"A\"", "\"AQ===\"").forEach {
            assertThrows(JsonParseException::class.java) { read(it) }
        }
    }

    @Test
    fun `invalid byte values and non-string scalars are rejected`() {
        listOf("[256]", "[-129]", "[1.5]", "[null]", "[true]", "[\"1\"]", "1", "true", "{}").forEach {
            assertThrows(JsonParseException::class.java) { read(it) }
        }
    }

    @Test
    fun `null bytes are written and read as null`() {
        assertEquals("null", chronicleGson.toJson(null, ByteArray::class.java))
        assertNull(chronicleGson.fromJson("null", ByteArray::class.java))
    }

    @Test
    fun `event or read model properties use both wire formats and round-trip`() {
        val content = Content(Duration.ofHours(1), byteArrayOf(1, 2, 3))
        val json = chronicleGson.toJson(content)
        assertEquals("""{"duration":"01:00:00","bytes":"AQID"}""", json)
        val read = chronicleGson.fromJson(json, Content::class.java)
        assertEquals(content.duration, read.duration)
        assertArrayEquals(content.bytes, read.bytes)
    }

    @Test
    fun `absent and null properties remain absent`() {
        assertEquals("{}", chronicleGson.toJson(Content()))
        val content = chronicleGson.fromJson("""{"duration":null,"bytes":null}""", Content::class.java)
        assertNull(content.duration)
        assertNull(content.bytes)
    }
}
