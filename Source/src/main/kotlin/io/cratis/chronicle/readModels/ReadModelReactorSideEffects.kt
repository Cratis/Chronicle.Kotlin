// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

/** Appends read-model reactor return values under one explicit operation context. */
internal class ReadModelReactorSideEffects(private val eventLog: IEventLog) {
    /**
     * Appends all valid side effects as one atomic batch.
     *
     * The 16.44.1 materialized read-model watch transport exposes correlation but not causation or
     * caused-by identity. [context] therefore preserves its correlation identifier while explicitly
     * identifying this client-side read-model reactor as the system boundary; no hidden request or
     * thread state is consulted.
     */
    suspend fun append(result: Any?, modelKey: String, context: OperationContext) {
        val sideEffects = when (result) {
            null, Unit -> emptyList()
            is EventForEventSourceId -> listOf(result)
            is List<*> -> result.filterNotNull().map { item ->
                if (item is EventForEventSourceId) item else EventForEventSourceId(modelKey, item)
            }
            else -> listOf(EventForEventSourceId(modelKey, result))
        }.filter { it.event::class.findAnnotation<EventType>() != null }

        if (sideEffects.isEmpty()) return

        val results = eventLog.appendMany(sideEffects, context)
        if (results.any { !it.isSuccess }) {
            val messages = results.flatMap { it.errors }.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException("Failed to append read model reactor side-effect events: $messages")
        }
    }
}
