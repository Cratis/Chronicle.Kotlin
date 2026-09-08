// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope

/** Stages operations for one explicit atomic batch. */
class EventSequenceOperations(
    override val eventSequence: IEventSequence,
    override val context: OperationContext
) : IEventSequenceOperations {
    private val eventSources = linkedMapOf<String, IEventSourceOperations>()

    override fun forEventSourceId(
        eventSourceId: String,
        configure: IEventSourceOperations.() -> Unit
    ): EventSequenceOperations {
        eventSources.getOrPut(eventSourceId) { EventSourceOperations() }.configure()
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
    }

    override suspend fun perform(): List<AppendResult> = eventSequence.appendMany(
        events = getEventsToAppend(),
        context = context,
        concurrencyScopes = eventSources
            .filterValues { it.concurrencyScope != ConcurrencyScope.notSet }
            .mapValues { (_, operations) -> operations.concurrencyScope }
    )
}
