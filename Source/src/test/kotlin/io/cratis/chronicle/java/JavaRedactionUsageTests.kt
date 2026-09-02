// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

class JavaRedactionUsageTests {
    @Test
    fun `blocking sequence exposes explicit context bulk redaction to ordinary Java`() {
        val sequence = mockk<IEventSequence>(relaxed = true)
        val context = OperationContext.system()

        JavaRedactionUsage.throughBlockingSequence(BlockingEventSequence(sequence), context, String::class.java)

        coVerify(exactly = 1) {
            sequence.redactForEventSource("source-1", RedactionReason("erasure"), context, listOf(String::class))
        }
    }

    @Test
    fun `event sequence bridge exposes explicit context bulk redaction to ordinary Java`() {
        val sequence = mockk<IEventSequence>(relaxed = true)
        val context = OperationContext.system()

        JavaRedactionUsage.throughBridge(sequence, context, String::class.java)

        coVerify(exactly = 1) {
            sequence.redactForEventSource("source-1", RedactionReason("erasure"), context, listOf(String::class))
        }
    }
}
