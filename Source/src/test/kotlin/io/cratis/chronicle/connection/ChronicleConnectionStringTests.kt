// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ChronicleConnectionStringTests {

    @Test
    fun `parses host and port from connection string`() {
        val cs = ChronicleConnectionString.parse("chronicle://myserver:12345")
        assertEquals("myserver", cs.host)
        assertEquals(12345, cs.port)
    }

    @Test
    fun `parses username and password`() {
        val cs = ChronicleConnectionString.parse("chronicle://alice:secret@myserver:35000")
        assertEquals("alice", cs.username)
        assertEquals("secret", cs.password)
    }

    @Test
    fun `parses disableTls flag`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000?disableTls=true")
        assertTrue(cs.disableTls)
    }

    @Test
    fun `disableTls is false by default`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000")
        assertFalse(cs.disableTls)
    }

    @Test
    fun `parses apiKey`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000?apiKey=my-key")
        assertEquals("my-key", cs.apiKey)
    }

    @Test
    fun `defaults to port 35000 when not specified`() {
        val cs = ChronicleConnectionString.parse("chronicle://localhost")
        assertEquals(35000, cs.port)
    }

    @Test
    fun `DEVELOPMENT preset has correct values`() {
        val cs = ChronicleConnectionString.DEVELOPMENT
        assertEquals("localhost", cs.host)
        assertEquals(35000, cs.port)
        assertTrue(cs.disableTls)
        assertEquals("chronicle-dev-client", cs.username)
    }

    @Test
    fun `target returns host colon port`() {
        val cs = ChronicleConnectionString.parse("chronicle://somehost:9090")
        assertEquals("somehost:9090", cs.target)
    }

    @Test
    fun `throws on non-chronicle scheme`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChronicleConnectionString.parse("http://localhost:35000")
        }
    }
}
