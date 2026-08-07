// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.artifacts.given.OrderPlaced
import io.cratis.chronicle.artifacts.given.OrderReactor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ChronicleOptionsTests {

    @Test
    fun `fromConnectionString parses connection string`() {
        val opts = ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
        assertEquals("localhost", opts.connectionString.host)
        assertEquals(35000, opts.connectionString.port)
    }

    @Test
    fun `development options point to localhost over TLS`() {
        val opts = ChronicleOptions.development()
        assertEquals("localhost", opts.connectionString.host)
        assertEquals(35000, opts.connectionString.port)
        assertFalse(opts.connectionString.disableTls)
    }

    @Test
    fun `programIdentifier defaults to Unknown`() {
        val opts = ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
        assertEquals("Unknown", opts.programIdentifier)
    }

    @Test
    fun `artifacts are discovered and registered automatically by default`() {
        assertTrue(ChronicleOptions.development().autoDiscoverAndRegister)
    }

    @Test
    fun `withoutAutoRegistration turns automatic registration off`() {
        assertFalse(ChronicleOptions.development().withoutAutoRegistration().autoDiscoverAndRegister)
    }

    @Test
    fun `withoutAutoRegistration leaves everything else alone`() {
        val opts = ChronicleOptions.development()
        val without = opts.withoutAutoRegistration()

        assertEquals(opts.connectionString, without.connectionString)
        assertEquals(opts.programIdentifier, without.programIdentifier)
        assertEquals(opts.defaultSinkTypeId, without.defaultSinkTypeId)
        assertSame(opts.artifacts, without.artifacts)
    }

    @Test
    fun `withArtifactsFrom narrows discovery to the given packages`() {
        val opts = ChronicleOptions.development().withArtifactsFrom("io.cratis.chronicle.artifacts.given")

        assertTrue(opts.artifacts.eventTypes.contains(OrderPlaced::class))
        assertTrue(opts.artifacts.reactors.contains(OrderReactor::class))
    }
}
