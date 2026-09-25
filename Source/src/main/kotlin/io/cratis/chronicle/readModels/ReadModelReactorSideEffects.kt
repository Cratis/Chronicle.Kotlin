// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

/**
 * Appends whatever a read model reactor handler returned, so a reactor never has to take a
 * dependency on the event log just to record what it decided.
 *
 * The conventions match reactor side effects: a single event object or an [EventForEventSourceId]
 * appends directly, and a `List` may mix bare events (appended to the changed instance's key) with
 * [EventForEventSourceId] wrappers carrying their own event source id. The events of a list are
 * appended as one atomic batch, even across event sources. Anything else - `Unit`, a
 * `null`, a value whose class carries no `@EventType` - is ignored, which is what makes handlers
 * that simply do their work and return nothing valid.
 */
internal class ReadModelReactorSideEffects(private val eventLog: IEventLog) {
    /**
     * Appends the events in [result], using [modelKey] as the event source id for any event that
     * does not name its own.
     */
    suspend fun append(result: Any?, modelKey: String) {
        val sideEffects = when (result) {
            null, Unit -> return
            is EventForEventSourceId -> listOf(result)
            is List<*> -> result.filterNotNull().map { item ->
                item as? EventForEventSourceId ?: EventForEventSourceId(modelKey, item)
            }
            else -> listOf(EventForEventSourceId(modelKey, result))
        }.filter { it.event::class.findAnnotation<EventType>() != null }

        val results = when (sideEffects.size) {
            0 -> return
            1 -> listOf(eventLog.append(sideEffects.single().eventSourceId, sideEffects.single().event))
            else -> eventLog.appendMany(sideEffects)
        }
        if (results.all { it.isSuccess }) return

        val messages = results
            .flatMap { r -> r.errors.map { it.message } + r.constraintViolations.map { it.message } }
            .distinct()
            .joinToString()
            .ifEmpty { "constraint violation" }
        val names = sideEffects.joinToString { it.event::class.simpleName ?: "event" }
        throw IllegalStateException("Failed to append read model reactor side-effect event(s) '$names': $messages")
    }
}
