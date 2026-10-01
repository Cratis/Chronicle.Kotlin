// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import bcl.Bcl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Converts a wire [Bcl.Guid] to a Java [UUID].
 *
 * A .NET `Guid` is mixed-endian: `Guid.ToByteArray()` writes Data1 (bytes 0-3), Data2 (bytes 4-5) and
 * Data3 (bytes 6-7) little-endian, and Data4 (bytes 8-15) as is. `bcl.Guid` is those 16 bytes split at
 * offset 8 into two little-endian `fixed64` values, `lo` and `hi`. A [UUID] is big-endian throughout
 * (RFC 4122), so the 16 raw bytes are recovered and the first three fields are swapped; the last eight
 * bytes are left alone.
 *
 * Reversing the bytes of each 64-bit half independently is not equivalent: it leaves Data1, Data2 and
 * Data3 byte-reversed.
 */
internal fun Bcl.Guid.toUuid(): UUID {
    val raw = ByteBuffer.allocate(GUID_SIZE).order(ByteOrder.LITTLE_ENDIAN).putLong(lo).putLong(hi).array()
    val uuid = swapFields(raw)
    val buffer = ByteBuffer.wrap(uuid).order(ByteOrder.BIG_ENDIAN)
    return UUID(buffer.long, buffer.long)
}

/**
 * Converts a Java [UUID] to a wire [Bcl.Guid]; the inverse of [toUuid].
 *
 * Writes the UUID's 16 big-endian RFC 4122 bytes, swaps Data1, Data2 and Data3 into .NET's
 * little-endian field order, leaves Data4 alone and reads the two halves as little-endian `fixed64`.
 */
internal fun UUID.toBclGuid(): Bcl.Guid {
    val uuid = ByteBuffer.allocate(GUID_SIZE)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
    val raw = ByteBuffer.wrap(swapFields(uuid)).order(ByteOrder.LITTLE_ENDIAN)

    return Bcl.Guid.newBuilder()
        .setLo(raw.long)
        .setHi(raw.long)
        .build()
}

/**
 * The UUID the kernel holds a value under when a client that predates the mixed-endian conversion sent it.
 *
 * Those clients wrote `lo` and `hi` as the byte-reversed big-endian halves of the UUID, so the kernel read
 * the 16 bytes of the UUID as they stood and showed Data1, Data2 and Data3 byte-reversed: sending
 * `01020304-0506-0708-090a-0b0c0d0e0f10` stored `04030201-0605-0807-090a-0b0c0d0e0f10`. The result is its
 * own inverse, and equals this UUID when the first three groups are palindromic.
 */
internal fun UUID.toTransposedUuid(): UUID =
    Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
        .build()
        .toUuid()

private const val GUID_SIZE = 16

/**
 * Swaps the endianness of the first three GUID fields (4, 2 and 2 bytes) and keeps the last eight
 * bytes. Swapping is its own inverse, so it converts in both directions.
 */
private fun swapFields(bytes: ByteArray): ByteArray {
    val swapped = bytes.copyOf()
    swapped[0] = bytes[3]; swapped[1] = bytes[2]; swapped[2] = bytes[1]; swapped[3] = bytes[0] // Data1
    swapped[4] = bytes[5]; swapped[5] = bytes[4] // Data2
    swapped[6] = bytes[7]; swapped[7] = bytes[6] // Data3
    return swapped
}
