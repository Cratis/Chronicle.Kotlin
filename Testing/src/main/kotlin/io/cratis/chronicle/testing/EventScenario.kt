// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.json.chronicleGson
import kotlin.reflect.KClass

/**
 * Specifies what a piece of code appends, with no kernel behind it.
 *
 * The point is not the assertions - JUnit and Kotest have those covered - it is that the code under
 * test runs against the real client surface with nothing to start up. No kernel, no container, no
 * MongoDB, no Docker: an ordinary unit test, in an ordinary unit test loop.
 *
 * ```kotlin
 * val scenario = EventScenario()
 *
 * Registrations(scenario.eventLog).register("employee-1", "Ada", "Lovelace")
 *
 * scenario.shouldHaveAppended<EmployeeHired>("employee-1") { it.firstName == "Ada" }
 * scenario.shouldHaveAppendedExactly(1)
 * ```
 *
 * Events are serialized through the client's own serializer on the way in, so an event that could
 * not survive a round trip to the kernel fails here too - which is most of what an append spec is
 * checking in the first place.
 *
 * @param eventStoreName The event store name stamped on event contexts.
 * @param namespace The namespace stamped on event contexts.
 */
class EventScenario(
    eventStoreName: String = "testing",
    namespace: String = "default"
) {
    /** The event log to hand whatever is under test. */
    val eventLog: InMemoryEventSequence = InMemoryEventSequence(
        eventStoreName = eventStoreName,
        namespace = namespace
    )

    /** The same, as the interface the code under test should be taking. */
    val eventSequence: IEventSequence get() = eventLog

    private var baseline = 0

    /** Events appended after the last [given] call; [eventLog] still retains the full history. */
    internal val actionEvents: List<AppendedEvent> get() = eventLog.events.drop(baseline)

    /** Forgets everything, so one scenario can serve several specs. */
    fun reset() {
        eventLog.clear()
        baseline = 0
    }

    /**
     * Appends [events] as preconditions - what was already true before the code under test ran.
     *
     * Kept separate from what the code under test appends so an assertion can talk about the latter
     * without counting the former.
     *
     * @param eventSourceId The event source the events belong to.
     * @param events The events to append.
     */
    suspend fun given(eventSourceId: String, vararg events: Any): List<AppendResult> =
        events.map { eventLog.append(eventSourceId, it) }.also { baseline = eventLog.count }

    /**
     * Appends one event with explicit options, for preconditions that need shaping - a stream, a
     * tag, a time it actually occurred.
     */
    suspend fun given(eventSourceId: String, event: Any, options: AppendOptions): AppendResult =
        eventLog.append(eventSourceId, event, options).also { baseline = eventLog.count }

    /** Every event of [eventClass] appended after the last [given] call, deserialized. */
    fun <T : Any> eventsOf(eventClass: KClass<T>): List<T> {
        val id = eventClass.eventTypeId()
        return actionEvents
            .filter { it.context.eventType.id.value == id }
            .map { chronicleGson.fromJson(it.content, eventClass.java) }
    }

    /** Every event of [eventClass] for [eventSourceId] appended after the last [given] call, deserialized. */
    fun <T : Any> eventsOf(eventClass: KClass<T>, eventSourceId: String): List<T> {
        val id = eventClass.eventTypeId()
        return actionEvents
            .filter { it.context.eventType.id.value == id && it.context.eventSourceId == eventSourceId }
            .map { chronicleGson.fromJson(it.content, eventClass.java) }
    }

    /** The event source ids that have had anything appended to them, in order of first appearance. */
    fun eventSourceIds(): List<String> = eventLog.events.map { it.context.eventSourceId }.distinct()
}

/** Every appended event of [T], deserialized. */
inline fun <reified T : Any> EventScenario.eventsOf(): List<T> = eventsOf(T::class)

/** Every appended event of [T] for [eventSourceId], deserialized. */
inline fun <reified T : Any> EventScenario.eventsOf(eventSourceId: String): List<T> =
    eventsOf(T::class, eventSourceId)
