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
 * [EventForEventSourceId] wrappers carrying their own event source id. Anything else - `Unit`, a
 * `null`, a value whose class carries no `@EventType` - is ignored, which is what makes handlers
 * that simply do their work and return nothing valid.
 */
internal class ReadModelReactorSideEffects(private val eventLog: IEventLog) {
    /**
     * Appends the events in [result], using [modelKey] as the event source id for any event that
     * does not name its own.
     */
    suspend fun append(result: Any?, modelKey: String) {
        when (result) {
            null, Unit -> return
            is EventForEventSourceId -> appendIfEventType(result.eventSourceId, result.event)
            is List<*> -> result.filterNotNull().forEach { item ->
                if (item is EventForEventSourceId) {
                    appendIfEventType(item.eventSourceId, item.event)
                } else {
                    appendIfEventType(modelKey, item)
                }
            }
            else -> appendIfEventType(modelKey, result)
        }
    }

    /** Appends [event] to [eventSourceId] when its class carries `@EventType`; ignores anything else. */
    private suspend fun appendIfEventType(eventSourceId: String, event: Any) {
        if (event::class.findAnnotation<EventType>() == null) return

        val result = eventLog.append(eventSourceId, event)
        if (!result.isSuccess) {
            val messages = result.errors.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException(
                "Failed to append read model reactor side-effect event '${event::class.simpleName}': $messages"
            )
        }
    }
}
