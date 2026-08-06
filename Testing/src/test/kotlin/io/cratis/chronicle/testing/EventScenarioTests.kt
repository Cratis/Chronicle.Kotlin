// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@EventType
data class EmployeeHired(val firstName: String = "", val lastName: String = "", val title: String = "")

@EventType
data class EmployeePromoted(val newTitle: String = "")

@EventType(id = "employee-left")
data class EmployeeLeft(val reason: String = "")

/** The kind of code a slice spec is actually about: something that decides what to append. */
private class Registrations(private val eventLog: IEventSequence) {
    suspend fun register(id: String, firstName: String, lastName: String) {
        eventLog.append(id, EmployeeHired(firstName, lastName, "Engineer"))
    }

    suspend fun promote(id: String, title: String) {
        eventLog.append(id, EmployeePromoted(title))
    }
}

/**
 * The value here is not the assertions - JUnit has those - it is that the code under test runs
 * against the real client surface with no kernel, no container and no database behind it.
 */
class EventScenarioTests {

    @Test
    fun `what the code under test appended is what comes back`() = runBlocking {
        val scenario = EventScenario()

        Registrations(scenario.eventLog).register("employee-1", "Ada", "Lovelace")

        val hired = scenario.shouldHaveAppended<EmployeeHired>("employee-1")
        assertEquals("Ada", hired.firstName)
        assertEquals("Lovelace", hired.lastName)
    }

    @Test
    fun `a condition on the event itself is part of the assertion`() = runBlocking {
        val scenario = EventScenario()
        Registrations(scenario.eventLog).register("employee-1", "Ada", "Lovelace")

        scenario.shouldHaveAppended<EmployeeHired> { it.title == "Engineer" }
    }

    @Test
    fun `an event source that was never touched has nothing`() = runBlocking {
        val scenario = EventScenario()
        Registrations(scenario.eventLog).register("employee-1", "Ada", "Lovelace")

        assertThrows(AssertionError::class.java) {
            scenario.shouldHaveAppended<EmployeeHired>("employee-2")
        }
    }

    @Test
    fun `a failure says what was actually appended`() = runBlocking {
        val scenario = EventScenario()
        Registrations(scenario.eventLog).promote("employee-1", "Principal")

        val error = assertThrows(AssertionError::class.java) {
            scenario.shouldHaveAppended<EmployeeHired>("employee-1")
        }

        assertTrue(error.message!!.contains("EmployeeHired"), error.message)
        assertTrue(error.message!!.contains("What was appended"), error.message)
        assertTrue(error.message!!.contains("EmployeePromoted"), error.message)
    }

    @Test
    fun `preconditions are appended like anything else`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))

        Registrations(scenario.eventLog).promote("employee-1", "Principal")

        scenario.shouldHaveAppendedExactly(2)
        scenario.shouldHaveAppended<EmployeePromoted>("employee-1") { it.newTitle == "Principal" }
    }

    @Test
    fun `counting is per event type as well as in total`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeHired(), EmployeePromoted(), EmployeePromoted())

        scenario.shouldHaveAppendedExactly(3)
        scenario.shouldHaveAppendedExactly<EmployeePromoted>(2)
        scenario.shouldNotHaveAppended<EmployeeLeft>()
    }

    @Test
    fun `nothing appended is something that can be asserted`() {
        EventScenario().shouldHaveAppendedNothing()
    }

    @Test
    fun `an event type declaring its own id is found under that id`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeLeft("moved on"))

        assertEquals("employee-left", scenario.eventLog.events.single().context.eventType.id.value)
        assertEquals("moved on", scenario.eventsOf<EmployeeLeft>().single().reason)
    }

    @Test
    fun `an event that is not an event type is rejected the way the kernel would reject it`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { EventScenario().given("employee-1", "not an event") }
        }
        assertTrue(error.message!!.contains("@EventType"))
    }

    @Test
    fun `shaping an append is carried onto the event context`() = runBlocking {
        val scenario = EventScenario()
        val occurred = Instant.parse("1998-06-01T09:00:00Z")

        scenario.given(
            "employee-1",
            EmployeeHired("Ada", "Lovelace", "Engineer"),
            AppendOptions(
                eventStreamType = "Onboarding",
                eventStreamId = "stream-1",
                eventSourceType = "Employee",
                tags = listOf("hr"),
                occurred = occurred
            )
        )

        val context = scenario.eventLog.events.single().context
        assertEquals("Onboarding", context.eventStreamType)
        assertEquals("stream-1", context.eventStreamId)
        assertEquals("Employee", context.eventSourceType)
        assertEquals(listOf("hr"), context.tags)
        assertEquals(occurred, context.occurred)
    }

    @Test
    fun `positions are assigned in order`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeHired(), EmployeePromoted())

        assertEquals(listOf(0L, 1L), scenario.eventLog.events.map { it.context.sequenceNumber })
        assertEquals(EventSequenceNumber(1), scenario.eventLog.getTailSequenceNumber())
    }

    @Test
    fun `reading back is the same surface the real client offers`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeHired("Ada"))
        scenario.given("employee-2", EmployeeHired("Grace"))

        assertTrue(scenario.eventLog.hasEventsFor("employee-1"))
        assertFalse(scenario.eventLog.hasEventsFor("employee-3"))
        assertEquals(1, scenario.eventLog.getForEventSourceIdAndEventTypes("employee-2", listOf(EmployeeHired::class)).size)
        assertEquals(listOf("employee-1", "employee-2"), scenario.eventSourceIds())
    }

    @Test
    fun `resetting forgets everything`() = runBlocking {
        val scenario = EventScenario()
        scenario.given("employee-1", EmployeeHired())

        scenario.reset()

        scenario.shouldHaveAppendedNothing()
    }
}
