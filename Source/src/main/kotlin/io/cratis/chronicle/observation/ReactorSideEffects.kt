// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

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
 * [EventForEventSourceId] wrappers naming their own. Anything else (`Unit`, `null`, a value whose
 * class carries no `@EventType`) is ignored, which is what makes a handler that simply does its work
 * and returns nothing valid.
 */
internal class ReactorSideEffects(private val eventLog: IEventLog) {
    /**
     * Appends the events in [result], using [triggeringEventSourceId] for any event that does not
     * name its own.
     */
    suspend fun append(result: Any?, triggeringEventSourceId: String) {
        when (result) {
            null, Unit -> return
            is EventForEventSourceId -> appendIfEventType(result)
            is List<*> -> result.filterNotNull().forEach { item ->
                if (item is EventForEventSourceId) {
                    appendIfEventType(item)
                } else {
                    appendIfEventType(EventForEventSourceId(triggeringEventSourceId, item))
                }
            }
            else -> appendIfEventType(EventForEventSourceId(triggeringEventSourceId, result))
        }
    }

    /**
     * Appends the event when its class carries `@EventType`; silently ignores anything else.
     *
     * The shaping the caller put on [EventForEventSourceId] is carried through, so a side effect can
     * target a stream, carry tags, name a subject or declare its own causation exactly as a direct
     * append can. Dropping it here would make those fields silently do nothing on this path.
     */
    private suspend fun appendIfEventType(sideEffect: EventForEventSourceId) {
        val event = sideEffect.event
        if (event::class.findAnnotation<EventType>() == null) return

        val result = eventLog.append(sideEffect.eventSourceId, event, sideEffect.toAppendOptions())
        if (!result.isSuccess) {
            val messages = result.errors.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException(
                "Failed to append reactor side-effect event '${event::class.simpleName}': $messages"
            )
        }
    }
}
