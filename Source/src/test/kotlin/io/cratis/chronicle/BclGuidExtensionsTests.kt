// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import bcl.Bcl
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Every expected value here is a literal captured from the .NET client (.NET 10 with protobuf-net 3.2),
 * never derived from the code under test - a round trip alone is satisfied by any invertible
 * transformation, including one that disagrees with .NET.
 */
class BclGuidExtensionsTests {

    private data class KnownGuid(val uuid: String, val lo: Long, val hi: Long)

    private val knownGuids = listOf(
        KnownGuid("01020304-0506-0708-090a-0b0c0d0e0f10", 0x0708050601020304L, 0x100f0e0d0c0b0a09L),
        KnownGuid("3f2a91c7-5d84-4e1b-9a06-b7e2c48d5f33", 0x4e1b5d843f2a91c7L, 0x335f8dc4e2b7069aL)
    )

    @Test
    fun `encodes a UUID the way the dotnet client would`() {
        for (known in knownGuids) {
            val wire = UUID.fromString(known.uuid).toBclGuid()

            assertEquals(known.lo, wire.lo, known.uuid)
            assertEquals(known.hi, wire.hi, known.uuid)
        }
    }

    @Test
    fun `decodes a bcl Guid produced by the dotnet client`() {
        for (known in knownGuids) {
            val wire = Bcl.Guid.newBuilder().setLo(known.lo).setHi(known.hi).build()

            assertEquals(UUID.fromString(known.uuid), wire.toUuid(), known.uuid)
        }
    }

    @Test
    fun `leaves hi as the byte reversed least significant bits because Data4 needs no field swap`() {
        val uuid = UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10")

        assertEquals(java.lang.Long.reverseBytes(uuid.leastSignificantBits), uuid.toBclGuid().hi)
    }

    @Test
    fun `writes the same bytes on the wire that protobuf-net writes for a dotnet Guid`() {
        // protobuf-net serializing a [ProtoMember] Guid of 01020304-0506-0708-090a-0b0c0d0e0f10 writes
        // 0a 12 (the member), then tag 1 fixed64 (09) and tag 2 fixed64 (11) - the two halves of
        // Guid.ToByteArray(). The bytes after the 0a 12 header are the bcl.Guid message itself.
        val expected = intArrayOf(
            0x09, 0x04, 0x03, 0x02, 0x01, 0x06, 0x05, 0x08, 0x07,
            0x11, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10
        ).map { it.toByte() }.toByteArray()

        val wire = UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10").toBclGuid()

        assertArrayEquals(expected, wire.toByteArray())
    }

    @Test
    fun `round trips a random UUID`() {
        repeat(1000) {
            val uuid = UUID.randomUUID()

            assertEquals(uuid, uuid.toBclGuid().toUuid())
        }
    }

    @Test
    fun `an id sent by an earlier client is held by the kernel with the first three groups byte reversed`() {
        // The expected value is the Guid string the .NET kernel showed for what the earlier client sent.
        val sent = UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10")

        assertEquals(UUID.fromString("04030201-0605-0807-090a-0b0c0d0e0f10"), sent.toTransposedUuid())
    }

    @Test
    fun `transposing twice gives the id back`() {
        repeat(1000) {
            val uuid = UUID.randomUUID()

            assertEquals(uuid, uuid.toTransposedUuid().toTransposedUuid())
        }
    }
}
