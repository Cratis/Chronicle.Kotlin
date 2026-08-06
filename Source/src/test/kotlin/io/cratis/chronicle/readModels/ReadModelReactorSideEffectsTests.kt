// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.eventSequences.AppendError
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
data class EmployeeWelcomed(val name: String)

@EventType
data class EmployeeAnnounced(val name: String)

class ReadModelReactorSideEffectsTests {

    private val succeeded = AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)

    private fun eventLog(result: AppendResult = succeeded): IEventLog = mockk<IEventLog>().also {
        coEvery { it.append(any(), any(), any()) } returns result
    }

    @Test
    fun `a returned event is appended to the changed instance key`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeWelcomed("Ada")

        ReadModelReactorSideEffects(eventLog).append(event, "employee-1")

        coVerify(exactly = 1) { eventLog.append("employee-1", event, null) }
    }

    @Test
    fun `every event in a returned list is appended`() = runBlocking {
        val eventLog = eventLog()

        ReadModelReactorSideEffects(eventLog)
            .append(listOf(EmployeeWelcomed("Ada"), EmployeeAnnounced("Ada")), "employee-1")

        coVerify(exactly = 2) { eventLog.append("employee-1", any(), null) }
    }

    @Test
    fun `an event naming its own event source id is appended there`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeAnnounced("Ada")

        ReadModelReactorSideEffects(eventLog).append(EventForEventSourceId("department-1", event), "employee-1")

        coVerify(exactly = 1) { eventLog.append("department-1", event, null) }
    }

    @Test
    fun `a return value that is not an event is ignored`() = runBlocking {
        val eventLog = eventLog()

        ReadModelReactorSideEffects(eventLog).append("just a string", "employee-1")

        coVerify(exactly = 0) { eventLog.append(any(), any(), any()) }
    }

    @Test
    fun `a handler that returned nothing appends nothing`() = runBlocking {
        val eventLog = eventLog()

        ReadModelReactorSideEffects(eventLog).append(Unit, "employee-1")
        ReadModelReactorSideEffects(eventLog).append(null, "employee-1")

        coVerify(exactly = 0) { eventLog.append(any(), any(), any()) }
    }

    @Test
    fun `a rejected append is surfaced rather than swallowed`() {
        val eventLog = eventLog(
            AppendResult(EventSequenceNumber(0), emptyList(), listOf(AppendError("boom")), false)
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { ReadModelReactorSideEffects(eventLog).append(EmployeeWelcomed("Ada"), "employee-1") }
        }
    }
}
