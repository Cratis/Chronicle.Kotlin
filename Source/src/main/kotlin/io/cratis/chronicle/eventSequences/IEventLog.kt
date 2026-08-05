// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import kotlinx.coroutines.flow.SharedFlow
import kotlin.reflect.KClass

interface IEventLog : IEventSequence {
    val transactional: ITransactionalEventSequence
    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult
    override suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult>
    override suspend fun hasEventsFor(eventSourceId: String): Boolean
    override suspend fun getTailSequenceNumber(eventSourceId: String?): EventSequenceNumber
    override suspend fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        eventTypes: List<KClass<*>>,
        eventStreamType: String?,
        eventStreamId: String?,
        eventSourceType: String?
    ): List<AppendedEvent>
    override suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String?,
        eventTypes: List<KClass<*>>?
    ): List<AppendedEvent>
    override suspend fun getNextSequenceNumber(): EventSequenceNumber
    override suspend fun getTailSequenceNumberForObserver(observerType: KClass<*>): EventSequenceNumber
    override suspend fun completeStream(eventStreamType: String, eventStreamId: String): CompleteStreamResult
    override suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason)
    override suspend fun redactForEventSource(eventSourceId: String, reason: RedactionReason, eventTypes: List<KClass<*>>)
    override val appendOperations: SharedFlow<List<AppendedEventWithResult>>
}
