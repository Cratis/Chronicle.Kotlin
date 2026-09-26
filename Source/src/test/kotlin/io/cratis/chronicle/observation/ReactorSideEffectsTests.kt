// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@EventType
data class SideEffectsStockDecreased(val isbn: String)

@EventType
data class SideEffectsReorderRequested(val isbn: String)

class ReactorSideEffectsTests {

    private val succeeded = AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)

    private fun eventLog(result: AppendResult = succeeded): IEventLog = mockk<IEventLog>().also {
        coEvery { it.append(any(), any(), any<AppendOptions>()) } returns result
        coEvery { it.appendMany(any<List<EventForEventSourceId>>(), any(), any()) } answers {
            firstArg<List<EventForEventSourceId>>().map { result }
        }
    }

    @Test
    fun `a single returned event is appended to the triggering event source`() = runBlocking {
        val eventLog = eventLog()
        val event = SideEffectsStockDecreased("isbn-1")

        ReactorSideEffects(eventLog).append(event, "book-1")

        coVerify(exactly = 1) { eventLog.append("book-1", event, any()) }
        coVerify(exactly = 0) { eventLog.appendMany(any<List<EventForEventSourceId>>(), any(), any()) }
    }

    @Test
    fun `a returned list is appended as one atomic batch across event sources`() = runBlocking {
        val eventLog = eventLog()
        val decreased = SideEffectsStockDecreased("isbn-1")
        val reorder = SideEffectsReorderRequested("isbn-1")

        ReactorSideEffects(eventLog).append(listOf(decreased, EventForEventSourceId("supplier-1", reorder)), "book-1")

        coVerify(exactly = 1) {
            eventLog.appendMany(
                match<List<EventForEventSourceId>> { batch ->
                    batch.map { it.eventSourceId to it.event } == listOf("book-1" to decreased, "supplier-1" to reorder)
                },
                any(),
                any()
            )
        }
        coVerify(exactly = 0) { eventLog.append(any(), any(), any<AppendOptions>()) }
    }

    @Test
    fun `values without an event type are left out of the batch`() = runBlocking {
        val eventLog = eventLog()
        val decreased = SideEffectsStockDecreased("isbn-1")

        ReactorSideEffects(eventLog).append(listOf(decreased, "not an event"), "book-1")

        coVerify(exactly = 1) { eventLog.append("book-1", decreased, any()) }
    }

    @Test
    fun `a rejected batch is surfaced rather than swallowed`() {
        val eventLog = eventLog(AppendResult(EventSequenceNumber(0), emptyList(), listOf(AppendError("boom")), false))

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                ReactorSideEffects(eventLog).append(
                    listOf(SideEffectsStockDecreased("isbn-1"), SideEffectsReorderRequested("isbn-1")),
                    "book-1"
                )
            }
        }
    }
}
