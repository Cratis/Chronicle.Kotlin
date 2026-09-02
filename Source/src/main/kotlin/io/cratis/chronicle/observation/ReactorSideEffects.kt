// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

/** Appends reactor return values with the metadata of the event that triggered them. */
internal class ReactorSideEffects(private val eventLog: IEventLog) {
    /** Appends all valid side effects as one batch under [triggeringContext]. */
    suspend fun append(result: Any?, triggeringContext: EventContext) {
        val sideEffects = when (result) {
            null, Unit -> emptyList()
            is EventForEventSourceId -> listOf(result)
            is List<*> -> result.filterNotNull().map { item ->
                if (item is EventForEventSourceId) item else EventForEventSourceId(triggeringContext.eventSourceId, item)
            }
            else -> listOf(EventForEventSourceId(triggeringContext.eventSourceId, result))
        }.filter { it.event::class.findAnnotation<EventType>() != null }

        if (sideEffects.isEmpty()) return

        val operationContext = OperationContext(
            triggeringContext.correlationId,
            triggeringContext.causation,
            triggeringContext.causedBy
        )
        val results = eventLog.appendMany(sideEffects, operationContext)
        if (results.any { !it.isSuccess }) {
            val messages = results.flatMap { it.errors }.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException("Failed to append reactor side-effect events: $messages")
        }
    }
}
