// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.auditing.Causation
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
import java.time.Instant
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
        coEvery { it.appendMany(any<List<EventForEventSourceId>>(), any(), any()) } answers {
            firstArg<List<EventForEventSourceId>>().map { result }
        }
    }

    @Test
    fun `a returned event is appended to the changed instance key`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeWelcomed("Ada")

        ReadModelReactorSideEffects(eventLog).append(event, "employee-1")

        coVerify(exactly = 1) { eventLog.append("employee-1", event, null) }
    }

    @Test
    fun `every event in a returned list is appended as one atomic batch`() = runBlocking {
        val eventLog = eventLog()
        val welcomed = EmployeeWelcomed("Ada")
        val announced = EmployeeAnnounced("Ada")

        ReadModelReactorSideEffects(eventLog)
            .append(listOf(welcomed, EventForEventSourceId("department-1", announced)), "employee-1")

        coVerify(exactly = 1) {
            eventLog.appendMany(
                match<List<EventForEventSourceId>> { batch ->
                    batch.map { it.eventSourceId to it.event } ==
                        listOf("employee-1" to welcomed, "department-1" to announced)
                },
                any(),
                any()
            )
        }
        coVerify(exactly = 0) { eventLog.append(any(), any(), any()) }
    }

    @Test
    fun `a rejected batch is surfaced rather than swallowed`() {
        val eventLog = eventLog(
            AppendResult(EventSequenceNumber(0), emptyList(), listOf(AppendError("boom")), false)
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                ReadModelReactorSideEffects(eventLog)
                    .append(listOf(EmployeeWelcomed("Ada"), EmployeeAnnounced("Ada")), "employee-1")
            }
        }
    }

    @Test
    fun `an event naming its own event source id is appended there`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeAnnounced("Ada")

        ReadModelReactorSideEffects(eventLog).append(EventForEventSourceId("department-1", event), "employee-1")

        coVerify(exactly = 1) { eventLog.append("department-1", event, EventForEventSourceId("department-1", event).toAppendOptions()) }
    }

    @Test
    fun `a single wrapped side effect retains all append options`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeAnnounced("Ada")
        val occurred = Instant.parse("2025-01-01T00:00:00Z")
        val causation = listOf(Causation.of(occurred, "import"))
        val sideEffect = EventForEventSourceId(
            "department-1", event, eventStreamType = "Announcements", eventStreamId = "stream-1",
            eventSourceType = "Department", tags = listOf("important"), subject = "employee-1", occurred = occurred,
            causation = causation
        )

        ReadModelReactorSideEffects(eventLog).append(sideEffect, "employee-1")

        coVerify(exactly = 1) {
            eventLog.append("department-1", event, AppendOptions(
                eventSourceType = "Department", eventStreamType = "Announcements", eventStreamId = "stream-1",
                tags = listOf("important"), subject = "employee-1", occurred = occurred, causation = causation
            ))
        }
    }

    @Test
    fun `a list containing one wrapped side effect retains its options`() = runBlocking {
        val eventLog = eventLog()
        val event = EmployeeAnnounced("Ada")
        val sideEffect = EventForEventSourceId("department-1", event, tags = listOf("important"))

        ReadModelReactorSideEffects(eventLog).append(listOf(sideEffect), "employee-1")

        coVerify(exactly = 1) { eventLog.append("department-1", event, sideEffect.toAppendOptions()) }
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
