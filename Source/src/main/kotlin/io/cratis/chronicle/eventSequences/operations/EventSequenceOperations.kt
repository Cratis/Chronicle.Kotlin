// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.util.UUID

/**
 * Implements [IEventSequenceOperations] by staging operations per event source in memory and
 * sending them to [eventSequence] as a single atomic append when [perform] runs.
 *
 * @param eventSequence The event sequence the operations are composed against.
 */
class EventSequenceOperations(override val eventSequence: IEventSequence) : IEventSequenceOperations {
    // Insertion-ordered so the events go on the wire in the order they were composed, which is what
    // makes the append order predictable for anyone reading them back.
    private val eventSources = linkedMapOf<String, IEventSourceOperations>()
    private var correlationId: UUID? = null

    override fun forEventSourceId(
        eventSourceId: String,
        configure: IEventSourceOperations.() -> Unit
    ): EventSequenceOperations {
        eventSources.getOrPut(eventSourceId) { EventSourceOperations() }.configure()
        return this
    }

    override fun withCorrelationId(correlationId: UUID): EventSequenceOperations {
        this.correlationId = correlationId
        return this
    }

    override fun getAppendedEvents(): List<Any> = eventSources.values.flatMap { it.getAppendedEvents() }

    override fun getEventsToAppend(): List<EventForEventSourceId> =
        eventSources.flatMap { (eventSourceId, operations) ->
            operations.getOperationsOfType<AppendOperation>().map { operation ->
                EventForEventSourceId(
                    eventSourceId = eventSourceId,
                    event = operation.event,
                    eventStreamType = operation.eventStreamType,
                    eventStreamId = operation.eventStreamId,
                    eventSourceType = operation.eventSourceType,
                    tags = operation.tags,
                    occurred = operation.occurred,
                    subject = operation.subject
                )
            }
        }

    override fun clear() {
        eventSources.clear()
        correlationId = null
    }

    override suspend fun perform(): List<AppendResult> = eventSequence.appendMany(
        events = getEventsToAppend(),
        // Only the event sources that asked for a check are sent. A notSet scope means the caller
        // never expressed an expectation, and sending it would ask the kernel to validate against
        // nothing rather than skip the check.
        concurrencyScopes = eventSources
            .filterValues { it.concurrencyScope != ConcurrencyScope.notSet }
            .mapValues { (_, operations) -> operations.concurrencyScope },
        correlationId = correlationId
    )
}
