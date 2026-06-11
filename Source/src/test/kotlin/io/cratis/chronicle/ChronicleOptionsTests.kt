// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

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
    fun `development options point to localhost with TLS disabled`() {
        val opts = ChronicleOptions.development()
        assertEquals("localhost", opts.connectionString.host)
        assertEquals(35000, opts.connectionString.port)
        assertTrue(opts.connectionString.disableTls)
    }

    @Test
    fun `programIdentifier defaults to Unknown`() {
        val opts = ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
        assertEquals("Unknown", opts.programIdentifier)
    }
}
