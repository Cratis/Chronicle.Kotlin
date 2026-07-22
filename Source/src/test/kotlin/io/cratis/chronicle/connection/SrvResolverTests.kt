// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.xbill.DNS.DClass
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.SRVRecord

private fun srvRecord(priority: Int, weight: Int, port: Int, target: String): SRVRecord = SRVRecord(
    Name.fromString("_chronicle._tcp.example.com."),
    DClass.IN,
    60L,
    priority,
    weight,
    port,
    Name.fromString("$target.")
)

class SrvResolverTests {

    @Test
    fun `resolves SRV records sorted by priority then weight`() {
        val resolver = SrvResolver(lookup = { _, _ ->
            arrayOf<Record>(
                srvRecord(priority = 10, weight = 0, port = 35000, target = "b.example.com"),
                srvRecord(priority = 0, weight = 5, port = 35001, target = "a.example.com"),
                srvRecord(priority = 0, weight = 10, port = 35002, target = "c.example.com")
            )
        })

        val addresses = runBlocking { resolver.resolve("example.com") }

        assertEquals(
            listOf(
                ChronicleServerAddress("c.example.com", 35002),
                ChronicleServerAddress("a.example.com", 35001),
                ChronicleServerAddress("b.example.com", 35000)
            ),
            addresses
        )
    }

    @Test
    fun `queries the _chronicle _tcp SRV name for the given host`() {
        var queriedName: String? = null
        val resolver = SrvResolver(lookup = { query, _ ->
            queriedName = query
            arrayOf<Record>(srvRecord(0, 0, 35000, "a.example.com"))
        })

        runBlocking { resolver.resolve("example.com") }

        assertEquals("_chronicle._tcp.example.com", queriedName)
    }

    @Test
    fun `passes the configured name server through to the lookup`() {
        var receivedNameServer: String? = null
        val resolver = SrvResolver(lookup = { _, nameServer ->
            receivedNameServer = nameServer
            arrayOf<Record>(srvRecord(0, 0, 35000, "a.example.com"))
        })

        runBlocking { resolver.resolve("example.com", nameServer = "1.1.1.1:5353") }

        assertEquals("1.1.1.1:5353", receivedNameServer)
    }

    @Test
    fun `throws a clear exception when there are no SRV records`() {
        val resolver = SrvResolver(lookup = { _, _ -> null })

        val exception = assertThrows(ChronicleSrvResolutionException::class.java) {
            runBlocking { resolver.resolve("example.com") }
        }
        assertTrue(exception.message!!.contains("example.com"))
    }

    @Test
    fun `throws a clear exception when the lookup returns an empty array`() {
        val resolver = SrvResolver(lookup = { _, _ -> emptyArray() })

        assertThrows(ChronicleSrvResolutionException::class.java) {
            runBlocking { resolver.resolve("example.com") }
        }
    }
}
