// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ReadModel
data class EmployeeState(
    val id: String = "",
    val firstName: String = "",
    val title: String = "",
    val leftBecause: String = ""
)

@Reducer
private class EmployeeStateReducer {
    fun hired(event: EmployeeHired) =
        EmployeeState(firstName = event.firstName, title = event.title)

    fun promoted(event: EmployeePromoted, state: EmployeeState?) =
        (state ?: EmployeeState()).copy(title = event.newTitle)

    fun left(event: EmployeeLeft, state: EmployeeState?, context: EventContext) =
        (state ?: EmployeeState()).copy(id = context.eventSourceId, leftBecause = event.reason)
}

@Reducer
private class SuspendingReducer {
    suspend fun hired(event: EmployeeHired): EmployeeState {
        delay(1)
        return EmployeeState(firstName = event.firstName)
    }
}

private class NotAReducer {
    fun somethingElse(name: String) = name
}

/**
 * A reducer is a fold: events in, read model out. Running that in-process is what makes "given these
 * three events, the state should be this" an ordinary unit test.
 */
class ReadModelScenarioTests {

    @Test
    fun `folding a stream produces the state at the end of it`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        val state = scenario.fold(
            "employee-1",
            EmployeeHired("Ada", "Lovelace", "Engineer"),
            EmployeePromoted("Principal Engineer")
        )

        assertEquals("Ada", state!!.firstName)
        assertEquals("Principal Engineer", state.title)
    }

    @Test
    fun `a handler taking only the event is folded too`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        val state = scenario.fold("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))

        assertEquals("Engineer", state!!.title)
    }

    @Test
    fun `a handler asking for the context gets one`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        val state = scenario.fold("employee-1", EmployeeLeft("moved on"))

        assertEquals("employee-1", state!!.id)
        assertEquals("moved on", state.leftBecause)
    }

    @Test
    fun `a suspending handler is awaited`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(SuspendingReducer())

        assertEquals("Ada", scenario.fold("employee-1", EmployeeHired("Ada"))!!.firstName)
    }

    @Test
    fun `state does not leak between event sources`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        scenario.fold("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))
        scenario.fold("employee-2", EmployeePromoted("Principal"))

        assertEquals("Engineer", scenario.stateFor("employee-1")!!.title)
        assertEquals("Principal", scenario.stateFor("employee-2")!!.title)
        assertEquals("", scenario.stateFor("employee-2")!!.firstName)
    }

    @Test
    fun `folding again continues from where it left off`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        scenario.fold("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))
        val state = scenario.fold("employee-1", EmployeePromoted("Principal"))

        assertEquals("Ada", state!!.firstName)
        assertEquals("Principal", state.title)
    }

    @Test
    fun `an event no handler wants leaves the state alone`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(SuspendingReducer())

        scenario.fold("employee-1", EmployeeHired("Ada"))
        val state = scenario.fold("employee-1", EmployeePromoted("Principal"))

        assertEquals("Ada", state!!.firstName)
    }

    @Test
    fun `an event source nothing was folded for has no state`() {
        assertNull(ReadModelScenario<EmployeeState>(EmployeeStateReducer()).stateFor("employee-9"))
    }

    @Test
    fun `every folded event is on the log, so a reducer spec can assert on both`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())

        scenario.fold("employee-1", EmployeeHired("Ada"), EmployeePromoted("Principal"))

        assertEquals(2, scenario.eventLog.count)
        assertEquals(listOf("employee-1"), scenario.eventSourceIds())
    }

    @Test
    fun `a class with no handlers is rejected rather than folding nothing forever`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ReadModelScenario<EmployeeState>(NotAReducer())
        }
        assertTrue(error.message!!.contains("no handler methods"))
    }

    @Test
    fun `resetting forgets the events and the state`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(EmployeeStateReducer())
        scenario.fold("employee-1", EmployeeHired("Ada"))

        scenario.reset()

        assertNull(scenario.stateFor("employee-1"))
        assertEquals(0, scenario.eventLog.count)
    }
}
