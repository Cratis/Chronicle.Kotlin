// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import kotlinx.coroutines.runBlocking

/** Java bridge for multi-source atomic append operations. */
object EventSequenceJavaBridge {
    /** Appends using a fresh per-call system context. */
    @JvmStatic
    @JvmOverloads
    fun appendMany(
        eventSequence: IEventSequence,
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap()
    ): List<AppendResult> = runBlocking { eventSequence.appendMany(events, concurrencyScopes) }

    /** Appends using explicit immutable metadata for the whole batch. */
    @JvmStatic
    @JvmOverloads
    fun appendMany(
        eventSequence: IEventSequence,
        events: List<EventForEventSourceId>,
        context: OperationContext,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap()
    ): List<AppendResult> = runBlocking { eventSequence.appendMany(events, context, concurrencyScopes) }

    /** Redacts matching events with a fresh per-call system context. */
    @JvmStatic
    @JvmOverloads
    fun redactForEventSource(
        eventSequence: IEventSequence,
        eventSourceId: String,
        reason: String,
        eventTypes: List<Class<*>> = emptyList()
    ) {
        runBlocking {
            eventSequence.redactForEventSource(eventSourceId, RedactionReason(reason), eventTypes = eventTypes.map { it.kotlin })
        }
    }

    /** Redacts matching events with explicit immutable metadata. */
    @JvmStatic
    @JvmOverloads
    fun redactForEventSource(
        eventSequence: IEventSequence,
        eventSourceId: String,
        reason: String,
        context: OperationContext,
        eventTypes: List<Class<*>> = emptyList()
    ) {
        runBlocking {
            eventSequence.redactForEventSource(eventSourceId, RedactionReason(reason), context, eventTypes.map { it.kotlin })
        }
    }
}
