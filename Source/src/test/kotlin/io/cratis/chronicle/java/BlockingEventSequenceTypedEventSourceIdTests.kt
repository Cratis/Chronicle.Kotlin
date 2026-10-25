// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

class BlockingEventSequenceTypedEventSourceIdTests {
    @Test
    fun `typed event source id overloads forward its value to the string members`() {
        val sequence = mockk<IEventSequence>()
        val result = mockk<AppendResult>()
        val options = AppendOptions()
        val event = Any()
        coEvery { sequence.append(any(), any(), any()) } returns result
        coEvery { sequence.appendMany("customer-1", any<List<Any>>(), any()) } returns listOf(result)
        coEvery { sequence.hasEventsFor(any()) } returns true
        coEvery { sequence.getTailSequenceNumber(any()) } returns EventSequenceNumber(7)
        coEvery { sequence.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns emptyList()
        coEvery { sequence.redactForEventSource(any(), any(), any()) } returns Unit

        JavaTypedEventSourceIdUsage.callEveryOverload(sequence, event, options)

        coVerify(exactly = 1) { sequence.append("customer-1", event, null) }
        coVerify(exactly = 1) { sequence.append("customer-1", event, options) }
        coVerify(exactly = 1) { sequence.appendMany("customer-1", listOf(event), null) }
        coVerify(exactly = 1) { sequence.appendMany("customer-1", listOf(event), options) }
        coVerify(exactly = 1) { sequence.hasEventsFor("customer-1") }
        coVerify(exactly = 1) { sequence.getTailSequenceNumber("customer-1") }
        coVerify(exactly = 1) {
            sequence.getForEventSourceIdAndEventTypes("customer-1", listOf(String::class), null, null, null)
        }
        coVerify(exactly = 1) {
            sequence.redactForEventSource("customer-1", RedactionReason("reason"), listOf(String::class))
        }
    }
}
