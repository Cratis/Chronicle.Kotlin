// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

private class UnregisteredSource

class InMemoryEventSourceRoutingTests {
    @Test
    fun `appending through an event source definition fails rather than dropping the metadata`() {
        val sequence = InMemoryEventSequence()

        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking { sequence.append("id", EmployeeHired(), AppendOptions(eventSource = UnregisteredSource::class)) }
        }
    }

    @Test
    fun `a batch with an event source routed event appends nothing`() = runBlocking {
        val sequence = InMemoryEventSequence()

        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                sequence.appendMany(
                    listOf(
                        EventForEventSourceId("id", EmployeeHired()),
                        EventForEventSourceId("id", EmployeeHired(), eventSource = UnregisteredSource::class)
                    )
                )
            }
        }

        assertFalse(sequence.hasEventsFor("id"))
    }
}
