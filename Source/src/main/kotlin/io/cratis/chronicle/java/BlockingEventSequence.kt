// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.transactions.UnitOfWork
import kotlinx.coroutines.runBlocking

/** Blocking, ordinary-Java view of an [IEventSequence]. */
class BlockingEventSequence(private val sequence: IEventSequence) {
    /** The suspending sequence underneath. */
    fun unwrap(): IEventSequence = sequence

    /** The sequence identifier. */
    val id: String get() = sequence.id.value

    /** Appends with a fresh per-call system context. */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        runBlocking { sequence.append(eventSourceId, event, options) }

    /** Appends with explicit immutable metadata. */
    @JvmOverloads
    fun append(
        eventSourceId: String,
        event: Any,
        context: OperationContext,
        options: AppendOptions? = null
    ): AppendResult = runBlocking { sequence.append(eventSourceId, event, context, options) }

    /** Appends one-source events atomically with a fresh per-call system context. */
    @JvmOverloads
    fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult> =
        runBlocking { sequence.appendMany(eventSourceId, events, options) }

    /** Appends one-source events atomically with explicit immutable metadata. */
    @JvmOverloads
    fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        context: OperationContext,
        options: AppendOptions? = null
    ): List<AppendResult> = runBlocking { sequence.appendMany(eventSourceId, events, context, options) }

    /** Appends a multi-source atomic batch with a fresh per-call system context. */
    fun appendMany(events: List<EventForEventSourceId>): List<AppendResult> =
        runBlocking { sequence.appendMany(events) }

    /** Appends a multi-source atomic batch with explicit metadata and concurrency scopes. */
    @JvmOverloads
    fun appendMany(
        events: List<EventForEventSourceId>,
        context: OperationContext,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap()
    ): List<AppendResult> = runBlocking { sequence.appendMany(events, context, concurrencyScopes) }

    /** Starts an explicit transaction bound to this sequence and [context]. */
    @JvmOverloads
    fun beginUnitOfWork(context: OperationContext = OperationContext.system()): BlockingUnitOfWork =
        BlockingUnitOfWork(UnitOfWork(sequence, context))

    /** Whether this sequence contains events for [eventSourceId]. */
    fun hasEventsFor(eventSourceId: String): Boolean = runBlocking { sequence.hasEventsFor(eventSourceId) }

    /** Tail sequence number, optionally narrowed to an event source. */
    @JvmOverloads
    fun getTailSequenceNumber(eventSourceId: String? = null): Long =
        runBlocking { sequence.getTailSequenceNumber(eventSourceId).value }

    /** Next sequence number. */
    fun getNextSequenceNumber(): Long = runBlocking { sequence.getNextSequenceNumber().value }

    /** Events of [eventTypes] for [eventSourceId]. */
    fun getForEventSourceIdAndEventTypes(eventSourceId: String, vararg eventTypes: Class<*>): List<AppendedEvent> =
        runBlocking { sequence.getForEventSourceIdAndEventTypes(eventSourceId, eventTypes.map { it.kotlin }) }

    /** Events from [sequenceNumber], optionally narrowed by source and tags. */
    @JvmOverloads
    fun getFromSequenceNumber(
        sequenceNumber: Long,
        eventSourceId: String? = null,
        tags: List<String> = emptyList()
    ): List<AppendedEvent> = runBlocking {
        sequence.getFromSequenceNumber(EventSequenceNumber(sequenceNumber), eventSourceId, tags = tags)
    }

    /** Redacts one event with a fresh per-call system context. */
    fun redact(sequenceNumber: Long, reason: String) {
        runBlocking { sequence.redact(EventSequenceNumber(sequenceNumber), RedactionReason(reason)) }
    }

    /** Redacts one event with explicit metadata. */
    fun redact(sequenceNumber: Long, reason: String, context: OperationContext) {
        runBlocking { sequence.redact(EventSequenceNumber(sequenceNumber), RedactionReason(reason), context) }
    }

    /** Redacts matching events with a fresh per-call system context. */
    fun redactForEventSource(eventSourceId: String, reason: String, vararg eventTypes: Class<*>) {
        runBlocking {
            sequence.redactForEventSource(eventSourceId, RedactionReason(reason), eventTypes = eventTypes.map { it.kotlin })
        }
    }

    /** Redacts matching events with explicit immutable metadata. */
    fun redactForEventSource(
        eventSourceId: String,
        reason: String,
        context: OperationContext,
        vararg eventTypes: Class<*>
    ) {
        runBlocking {
            sequence.redactForEventSource(
                eventSourceId,
                RedactionReason(reason),
                context,
                eventTypes.map { it.kotlin }
            )
        }
    }
}
