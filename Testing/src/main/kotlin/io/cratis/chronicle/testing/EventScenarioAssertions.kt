// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("EventScenarioAssertions")

package io.cratis.chronicle.testing

import kotlin.reflect.KClass

/**
 * Assertions over what an [EventScenario] appended after its last `given` call.
 *
 * They exist for the failure message rather than for the check: "expected one EmployeeHired for
 * 'employee-1', found none - 2 events were appended: EmployeePromoted for 'employee-2',
 * EmployeeLeft for 'employee-3'" tells you what happened. `assertTrue(events.any { ... })` tells you
 * `false`.
 */

/** Fails unless exactly [count] events were appended in total. */
fun EventScenario.shouldHaveAppendedExactly(count: Int) {
    if (actionEvents.size != count) {
        throw AssertionError("Expected exactly $count event(s) to be appended, but ${actionEvents.size} were.${appendedSummary()}")
    }
}

/** Fails unless nothing at all was appended. */
fun EventScenario.shouldHaveAppendedNothing() {
    if (actionEvents.isNotEmpty()) {
        throw AssertionError("Expected nothing to be appended, but ${actionEvents.size} event(s) were.${appendedSummary()}")
    }
}

/**
 * Fails unless at least one [eventClass] was appended for [eventSourceId] satisfying [matching].
 *
 * @param eventClass The event type expected.
 * @param eventSourceId The event source it should belong to, or `null` for any.
 * @param matching An extra condition on the event itself. Defaults to accepting any.
 */
fun <T : Any> EventScenario.shouldHaveAppended(
    eventClass: KClass<T>,
    eventSourceId: String? = null,
    matching: (T) -> Boolean = { true }
): T {
    val candidates =
        if (eventSourceId == null) eventsOf(eventClass) else eventsOf(eventClass, eventSourceId)

    return candidates.firstOrNull(matching)
        ?: throw AssertionError(
            "Expected a ${eventClass.simpleName}" +
                (eventSourceId?.let { " for '$it'" } ?: "") +
                (if (candidates.isEmpty()) ", but none was appended." else " matching the condition, but none of the ${candidates.size} appended did.") +
                appendedSummary()
        )
}

/** Fails unless exactly [count] events of [eventClass] were appended. */
fun <T : Any> EventScenario.shouldHaveAppendedExactly(eventClass: KClass<T>, count: Int) {
    val actual = eventsOf(eventClass).size
    if (actual != count) {
        throw AssertionError(
            "Expected exactly $count ${eventClass.simpleName} event(s), but $actual were appended.${appendedSummary()}"
        )
    }
}

/** Fails when any event of [eventClass] was appended. */
fun <T : Any> EventScenario.shouldNotHaveAppended(eventClass: KClass<T>) {
    val actual = eventsOf(eventClass).size
    if (actual != 0) {
        throw AssertionError(
            "Expected no ${eventClass.simpleName} to be appended, but $actual were.${appendedSummary()}"
        )
    }
}

/** Fails unless at least one [T] was appended for [eventSourceId] satisfying [matching]. */
inline fun <reified T : Any> EventScenario.shouldHaveAppended(
    eventSourceId: String? = null,
    noinline matching: (T) -> Boolean = { true }
): T = shouldHaveAppended(T::class, eventSourceId, matching)

/**
 * Fails unless exactly [count] events of [T] were appended.
 *
 * The JVM name differs from the untyped overload because a reified type parameter erases, leaving
 * the two with the same signature.
 */
@JvmName("shouldHaveAppendedExactlyOfType")
inline fun <reified T : Any> EventScenario.shouldHaveAppendedExactly(count: Int) =
    shouldHaveAppendedExactly(T::class, count)

/** Fails when any event of [T] was appended. */
inline fun <reified T : Any> EventScenario.shouldNotHaveAppended() = shouldNotHaveAppended(T::class)

/**
 * What was actually appended, for the tail of a failure message.
 *
 * The whole point of these assertions is that a failure says what happened, so this is the part that
 * earns them.
 */
internal fun EventScenario.appendedSummary(): String {
    if (actionEvents.isEmpty()) return " Nothing was appended."

    val lines = actionEvents.joinToString("\n") { event ->
        "  ${event.context.sequenceNumber}: ${event.context.eventType.id.value} " +
            "for '${event.context.eventSourceId}' ${event.content}"
    }
    return "\nWhat was appended:\n$lines"
}
