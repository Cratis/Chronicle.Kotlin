// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@EventType
data class EmployeeWelcomed(val name: String)

@EventType
data class EmployeeAnnounced(val name: String)

class ReadModelReactorSideEffectsTests {
    private val succeeded = AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)
    private val context = OperationContext.system()

    private fun eventLog(result: AppendResult = succeeded): IEventLog = mockk<IEventLog>().also {
        coEvery { it.appendMany(any(), any<OperationContext>(), any()) } returns listOf(result)
    }

    @Test
    fun `returned events are appended atomically in order under the supplied context`() = runBlocking {
        val eventLog = eventLog()
        val events = slot<List<EventForEventSourceId>>()
        coEvery { eventLog.appendMany(capture(events), context, any()) } returns listOf(succeeded, succeeded)

        ReadModelReactorSideEffects(eventLog).append(
            listOf(EmployeeWelcomed("Ada"), EventForEventSourceId("department-1", EmployeeAnnounced("Ada"))),
            "employee-1",
            context
        )

        coVerify(exactly = 1) { eventLog.appendMany(any(), context, any()) }
        assertEquals(listOf("employee-1", "department-1"), events.captured.map { it.eventSourceId })
    }

    @Test
    fun `non events and unit append nothing`() = runBlocking {
        val eventLog = eventLog()
        ReadModelReactorSideEffects(eventLog).append("not-event", "employee-1", context)
        ReadModelReactorSideEffects(eventLog).append(Unit, "employee-1", context)
        coVerify(exactly = 0) { eventLog.appendMany(any(), any<OperationContext>(), any()) }
    }

    @Test
    fun `a rejected append is surfaced`() {
        val eventLog = eventLog(AppendResult(EventSequenceNumber.unavailable, emptyList(), listOf(AppendError("boom")), false))
        assertThrows(IllegalStateException::class.java) {
            runBlocking { ReadModelReactorSideEffects(eventLog).append(EmployeeWelcomed("Ada"), "employee-1", context) }
        }
    }
}
