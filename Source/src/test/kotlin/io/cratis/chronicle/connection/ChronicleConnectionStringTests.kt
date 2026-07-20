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
        assertFalse(cs.disableTls)
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

    // Multi-host parsing

    @Test
    fun `parses multiple hosts into the addresses list`() {
        val cs = ChronicleConnectionString.parse("chronicle://host1:35000,host2:35001,host3:35002")
        assertEquals(
            listOf(
                ChronicleServerAddress("host1", 35000),
                ChronicleServerAddress("host2", 35001),
                ChronicleServerAddress("host3", 35002)
            ),
            cs.addresses
        )
    }

    @Test
    fun `host and port convenience accessors reflect the first address`() {
        val cs = ChronicleConnectionString.parse("chronicle://host1:35000,host2:35001")
        assertEquals("host1", cs.host)
        assertEquals(35000, cs.port)
    }

    @Test
    fun `multi-host entries without an explicit port default to 35000`() {
        val cs = ChronicleConnectionString.parse("chronicle://host1,host2:9000")
        assertEquals(
            listOf(ChronicleServerAddress("host1", 35000), ChronicleServerAddress("host2", 9000)),
            cs.addresses
        )
    }

    @Test
    fun `parses username and password ahead of a multi-host list`() {
        val cs = ChronicleConnectionString.parse("chronicle://alice:secret@host1:35000,host2:35001")
        assertEquals("alice", cs.username)
        assertEquals("secret", cs.password)
        assertEquals(2, cs.addresses.size)
    }

    @Test
    fun `parses options following a multi-host list`() {
        val cs = ChronicleConnectionString.parse("chronicle://host1:35000,host2:35001?disableTls=true&apiKey=my-key")
        assertEquals(2, cs.addresses.size)
        assertTrue(cs.disableTls)
        assertEquals("my-key", cs.apiKey)
    }

    // IPv6 bracket notation

    @Test
    fun `parses an IPv6 host in bracket notation with an explicit port`() {
        val cs = ChronicleConnectionString.parse("chronicle://[::1]:35000")
        assertEquals("::1", cs.host)
        assertEquals(35000, cs.port)
    }

    @Test
    fun `parses an IPv6 host in bracket notation without an explicit port`() {
        val cs = ChronicleConnectionString.parse("chronicle://[2001:db8::1]")
        assertEquals("2001:db8::1", cs.host)
        assertEquals(35000, cs.port)
    }

    @Test
    fun `parses a mix of IPv6 and regular hosts in a multi-host list`() {
        val cs = ChronicleConnectionString.parse("chronicle://[::1]:35000,host2:35001")
        assertEquals(
            listOf(ChronicleServerAddress("::1", 35000), ChronicleServerAddress("host2", 35001)),
            cs.addresses
        )
    }

    @Test
    fun `target renders an IPv6 address in bracket notation`() {
        val cs = ChronicleConnectionString.parse("chronicle://[::1]:35000")
        assertEquals("[::1]:35000", cs.target)
    }

    // loadBalancer

    @Test
    fun `loadBalancer defaults to least-connections`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000")
        assertEquals(LoadBalancer.LEAST_CONNECTIONS, cs.loadBalancer)
    }

    @Test
    fun `parses loadBalancer round-robin`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000?loadBalancer=round-robin")
        assertEquals(LoadBalancer.ROUND_ROBIN, cs.loadBalancer)
    }

    @Test
    fun `parses loadBalancer random`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000?loadBalancer=random")
        assertEquals(LoadBalancer.RANDOM, cs.loadBalancer)
    }

    @Test
    fun `throws on an unknown loadBalancer value`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChronicleConnectionString.parse("chronicle://host:35000?loadBalancer=bogus")
        }
    }

    // chronicle+srv

    @Test
    fun `parses a chronicle+srv connection string`() {
        val cs = ChronicleConnectionString.parse("chronicle+srv://example.com")
        assertTrue(cs.isSrv)
        assertEquals("example.com", cs.host)
    }

    @Test
    fun `chronicle+srv is not flagged as srv for a plain chronicle scheme`() {
        val cs = ChronicleConnectionString.parse("chronicle://example.com")
        assertFalse(cs.isSrv)
    }

    @Test
    fun `parses options on a chronicle+srv connection string`() {
        val cs = ChronicleConnectionString.parse("chronicle+srv://example.com?srvNameServer=1.1.1.1&loadBalancer=random")
        assertEquals("1.1.1.1", cs.srvNameServer)
        assertEquals(LoadBalancer.RANDOM, cs.loadBalancer)
    }

    @Test
    fun `throws when chronicle+srv specifies multiple hosts`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChronicleConnectionString.parse("chronicle+srv://host1,host2")
        }
    }

    @Test
    fun `srvNameServer is null by default`() {
        val cs = ChronicleConnectionString.parse("chronicle://host:35000")
        assertNull(cs.srvNameServer)
    }
}
