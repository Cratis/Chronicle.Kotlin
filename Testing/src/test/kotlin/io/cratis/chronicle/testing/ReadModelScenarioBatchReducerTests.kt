// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.observation.BatchContext
import io.cratis.chronicle.observation.EventBatch
import io.cratis.chronicle.observation.Observes
import io.cratis.chronicle.observation.Reducer
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Reducer
private class BatchEmployeeStateReducer {
    val calls = mutableListOf<Int>()
    var lastContext: BatchContext? = null

    @Observes(EmployeeHired::class)
    @Observes(EmployeePromoted::class)
    fun reduce(batch: EventBatch, current: EmployeeState?, context: BatchContext): EmployeeState? {
        calls.add(batch.size)
        lastContext = context
        return batch.fold(current) { state, entry ->
            when (val event = entry.event) {
                is EmployeeHired -> EmployeeState(id = entry.context.eventSourceId, firstName = event.firstName, title = event.title)
                is EmployeePromoted -> (state ?: EmployeeState()).copy(title = event.newTitle)
                else -> state
            }
        }
    }
}

@Reducer
private class DeferringReducer {
    @Observes(EmployeeHired::class)
    fun reduce(batch: EventBatch, current: EmployeeState?): EmployeeState? {
        batch.partitions.forEach { batch.defer(it, Duration.ofSeconds(30)) }
        return EmployeeState(firstName = "never written")
    }
}

private class Unfinished(message: String) : Exception(message)

@Reducer
private class FailingReducer {
    @Observes(EmployeeHired::class)
    fun reduce(batch: EventBatch, current: EmployeeState?): EmployeeState? {
        batch.partitions.forEach { batch.fail(it, Unfinished("cannot reduce $it")) }
        return current
    }
}

@Reducer
private class MixedReducer {
    fun hired(event: EmployeeHired) = EmployeeState(firstName = event.firstName)

    @Observes(EmployeePromoted::class)
    fun reduce(batch: EventBatch, current: EmployeeState?): EmployeeState? = current
}

/**
 * A batch reducer takes a page of events per call. The scenario has to run it the way Chronicle does -
 * one call with every event it observes - and honor what it reports, or a spec of one would pass against
 * behavior the kernel never shows.
 */
class ReadModelScenarioBatchReducerTests {

    @Test
    fun `a batch reducer is called once with every observed event of the fold`() = runBlocking {
        val reducer = BatchEmployeeStateReducer()
        val scenario = ReadModelScenario<EmployeeState>(reducer)

        val state = scenario.fold(
            "employee-1",
            EmployeeHired("Ada", "Lovelace", "Engineer"),
            EmployeeLeft("not observed"),
            EmployeePromoted("Principal Engineer"),
        )

        assertEquals(listOf(2), reducer.calls)
        assertEquals(EmployeeState(id = "employee-1", firstName = "Ada", title = "Principal Engineer"), state)
    }

    @Test
    fun `a batch reducer continues from the state of the previous fold`() = runBlocking {
        val scenario = ReadModelScenario<EmployeeState>(BatchEmployeeStateReducer())
        scenario.fold("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))

        val state = scenario.fold("employee-1", EmployeePromoted("Principal Engineer"))

        assertEquals("Ada", state!!.firstName)
        assertEquals("Principal Engineer", state.title)
    }

    @Test
    fun `a batch reducer is handed the page it is folding`() = runBlocking {
        val reducer = BatchEmployeeStateReducer()
        val scenario = ReadModelScenario<EmployeeState>(reducer)

        scenario.fold("employee-1", EmployeeHired("Ada"), EmployeePromoted("Lead"))

        val context = reducer.lastContext!!
        assertEquals(listOf("employee-1"), context.partitions)
        assertEquals(2, context.eventCount)
        assertEquals(0L, context.firstSequenceNumber)
        assertEquals(1L, context.lastSequenceNumber)
    }

    @Test
    fun `a fold with nothing the batch method observes leaves the state alone`() = runBlocking {
        val reducer = BatchEmployeeStateReducer()
        val scenario = ReadModelScenario<EmployeeState>(reducer)

        val state = scenario.fold("employee-1", EmployeeLeft("not observed"))

        assertNull(state)
        assertTrue(reducer.calls.isEmpty())
    }

    @Test
    fun `a deferred event source throws and writes no state`() {
        val scenario = ReadModelScenario<EmployeeState>(DeferringReducer())

        val error = assertThrows(ReducerDeferred::class.java) {
            runBlocking { scenario.fold("employee-1", EmployeeHired("Ada")) }
        }

        assertEquals(Duration.ofSeconds(30), error.retryAfter)
        assertEquals("employee-1", error.eventSourceId)
        assertNull(scenario.stateFor("employee-1"))
    }

    @Test
    fun `a failed event source rethrows what the reducer reported and writes no state`() {
        val scenario = ReadModelScenario<EmployeeState>(FailingReducer())

        val error = assertThrows(Unfinished::class.java) {
            runBlocking { scenario.fold("employee-1", EmployeeHired("Ada")) }
        }

        assertEquals("cannot reduce employee-1", error.message)
        assertNull(scenario.stateFor("employee-1"))
    }

    @Test
    fun `a reducer mixing a batch method with per-event handlers is refused`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ReadModelScenario<EmployeeState>(MixedReducer())
        }

        assertTrue(error.message!!.contains("mixes a batch method"))
    }
}
