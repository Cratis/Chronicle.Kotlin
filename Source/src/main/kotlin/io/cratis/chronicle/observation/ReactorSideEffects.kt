// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

/**
 * Appends whatever a reactor handler returned, so a reactor never has to take a dependency on the
 * event log just to record what it decided.
 *
 * A single event object or an [EventForEventSourceId] appends directly, and a `List` may freely mix
 * bare events - appended against the event source that triggered the reactor - with
 * [EventForEventSourceId] wrappers naming their own. The events of a list are appended as one atomic
 * batch, even across event sources: either all of them land or none do. Anything else (`Unit`,
 * `null`, a value whose class carries no `@EventType`) is ignored, which is what makes a handler that
 * simply does its work and returns nothing valid.
 */
internal class ReactorSideEffects(private val eventLog: IEventLog) {
    /**
     * Appends the events in [result], using [triggeringEventSourceId] for any event that does not
     * name its own.
     */
    suspend fun append(result: Any?, triggeringEventSourceId: String) {
        val sideEffects = when (result) {
            null, Unit -> return
            is EventForEventSourceId -> listOf(result)
            is List<*> -> result.filterNotNull().map { item ->
                item as? EventForEventSourceId ?: EventForEventSourceId(triggeringEventSourceId, item)
            }
            else -> listOf(EventForEventSourceId(triggeringEventSourceId, result))
        }.filter { it.event::class.findAnnotation<EventType>() != null }

        when (sideEffects.size) {
            0 -> return
            1 -> {
                // The shaping the caller put on EventForEventSourceId is carried through, so a side
                // effect can target a stream, carry tags, name a subject or declare its own causation
                // exactly as a direct append can.
                val sideEffect = sideEffects.single()
                val result = eventLog.append(sideEffect.eventSourceId, sideEffect.event, sideEffect.toAppendOptions())
                ensureSucceeded(listOf(result), sideEffects)
            }
            // One atomic batch, which carries each event's own shaping, so a returned list either lands
            // as a whole or not at all - the same guarantee the .NET client gives.
            else -> ensureSucceeded(eventLog.appendMany(sideEffects), sideEffects)
        }
    }

    private fun ensureSucceeded(results: List<AppendResult>, sideEffects: List<EventForEventSourceId>) {
        if (results.all { it.isSuccess }) return

        val messages = results
            .flatMap { result -> result.errors.map { it.message } + result.constraintViolations.map { it.message } }
            .distinct()
            .joinToString()
            .ifEmpty { "constraint violation" }
        val names = sideEffects.joinToString { it.event::class.simpleName ?: "event" }
        throw IllegalStateException("Failed to append reactor side-effect event(s) '$names': $messages")
    }
}
