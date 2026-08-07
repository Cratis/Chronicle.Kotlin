// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * An event sequence for Java, without the coroutines.
 *
 * Every method here is the suspending one on [IEventSequence] with the waiting done for you, so a
 * Java caller writes `eventLog.append("employee-1", new EmployeeHired(...))` rather than assembling
 * a continuation by hand.
 *
 * Each call blocks the calling thread until the kernel answers. That is what a Java caller almost
 * always wants - a controller method, a `main`, a scheduled job - but it does mean these must not be
 * called from inside a coroutine. Kotlin callers should use [IEventSequence] directly.
 *
 * @param sequence The sequence to forward to.
 */
class BlockingEventSequence(private val sequence: IEventSequence) {

    /** The suspending sequence underneath, for anything this does not cover. */
    fun unwrap(): IEventSequence = sequence

    /** The identifier of the sequence. */
    val id: String get() = sequence.id.value

    /**
     * Appends [event] against [eventSourceId].
     *
     * @param eventSourceId The event source the event belongs to.
     * @param event The event. Its class must carry `@EventType`.
     * @return What the kernel made of it, including any constraint violation.
     */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        runBlocking { sequence.append(eventSourceId, event, options) }

    /** Appends [events] against [eventSourceId] as one atomic batch. */
    @JvmOverloads
    fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions? = null
    ): List<AppendResult> = runBlocking { sequence.appendMany(eventSourceId, events, options) }

    /** Appends [events], each naming its own event source, as one atomic batch. */
    fun appendMany(events: List<EventForEventSourceId>): List<AppendResult> =
        runBlocking { sequence.appendMany(events) }

    /** Whether the sequence holds any event for [eventSourceId]. */
    fun hasEventsFor(eventSourceId: String): Boolean = runBlocking { sequence.hasEventsFor(eventSourceId) }

    /** The position of the last event in the sequence, or in [eventSourceId] when given. */
    @JvmOverloads
    fun getTailSequenceNumber(eventSourceId: String? = null): Long =
        runBlocking { sequence.getTailSequenceNumber(eventSourceId).value }

    /** The position the next append will land at. */
    fun getNextSequenceNumber(): Long = runBlocking { sequence.getNextSequenceNumber().value }

    /** The events of [eventTypes] appended for [eventSourceId]. */
    fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        vararg eventTypes: Class<*>
    ): List<AppendedEvent> = runBlocking {
        sequence.getForEventSourceIdAndEventTypes(eventSourceId, eventTypes.map { it.kotlin })
    }

    /** The events from [sequenceNumber] onwards. */
    @JvmOverloads
    fun getFromSequenceNumber(sequenceNumber: Long, eventSourceId: String? = null): List<AppendedEvent> =
        runBlocking { sequence.getFromSequenceNumber(EventSequenceNumber(sequenceNumber), eventSourceId) }

    /** Redacts the event at [sequenceNumber]. */
    fun redact(sequenceNumber: Long, reason: String) {
        runBlocking { sequence.redact(EventSequenceNumber(sequenceNumber), RedactionReason(reason)) }
    }

    /** Redacts every event appended for [eventSourceId], or only those of [eventTypes]. */
    fun redactForEventSource(eventSourceId: String, reason: String, vararg eventTypes: Class<*>) {
        runBlocking {
            sequence.redactForEventSource(
                eventSourceId,
                RedactionReason(reason),
                eventTypes.map { it.kotlin as KClass<*> }
            )
        }
    }
}
