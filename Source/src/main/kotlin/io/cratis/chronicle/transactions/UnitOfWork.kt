// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.correlation.CorrelationId
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation

/**
 * Represents an implementation of [IUnitOfWork].
 *
 * @property correlationId The [CorrelationId] for this [IUnitOfWork].
 * @param eventStore The [IEventStore] used to resolve event sequences when committing.
 * @param onCompleted Callback invoked when this [IUnitOfWork] completes.
 */
class UnitOfWork(
    override val correlationId: CorrelationId = CorrelationId.create(),
    private val eventStore: IEventStore,
    onCompleted: (IUnitOfWork) -> Unit = {}
) : IUnitOfWork {

    private data class StagedEvent(
        val eventSequenceId: EventSequenceId,
        val eventSourceId: String,
        val event: Any,
        val options: AppendOptions?
    )

    private val _stagedEvents = mutableListOf<StagedEvent>()
    private val _onCompleted = mutableListOf(onCompleted)
    private var _appendResults: List<AppendResult> = emptyList()
    private var _lastCommittedEventSequenceNumber: EventSequenceNumber? = null

    var isCommitted: Boolean = false
        private set

    private var isRolledBack: Boolean = false

    override val isCompleted: Boolean get() = isCommitted || isRolledBack

    override val isSuccess: Boolean get() = _appendResults.all { it.isSuccess }

    override fun addEvent(eventSequenceId: EventSequenceId, eventSourceId: String, event: Any, options: AppendOptions?) {
        _stagedEvents.add(StagedEvent(eventSequenceId, eventSourceId, event, options))
    }

    override fun getEvents(): List<Any> = _stagedEvents.map { it.event }

    override fun getConstraintViolations(): List<ConstraintViolation> = _appendResults.flatMap { it.constraintViolations }

    override fun getConcurrencyViolations(): List<ConcurrencyViolation> = _appendResults.mapNotNull { it.concurrencyViolation }

    override fun getAppendErrors(): List<AppendError> = _appendResults.flatMap { it.errors }

    override suspend fun commit() {
        try {
            if (_stagedEvents.isNotEmpty()) {
                _appendResults = appendStagedEventsGroupedByStream()
                _lastCommittedEventSequenceNumber = _appendResults
                    .filter { it.sequenceNumber.isActualValue }
                    .maxByOrNull { it.sequenceNumber.value }
                    ?.sequenceNumber
            }
        } finally {
            isCommitted = true
            _onCompleted.forEach { it(this) }
        }
    }

    override suspend fun rollback() {
        isRolledBack = true
        _stagedEvents.clear()
        _appendResults = emptyList()
        _onCompleted.forEach { it(this) }
    }

    override fun onCompleted(callback: (IUnitOfWork) -> Unit) {
        _onCompleted.add(callback)
    }

    override fun tryGetLastCommittedEventSequenceNumber(): EventSequenceNumber? = _lastCommittedEventSequenceNumber

    /**
     * Appends the staged events, grouped into consecutive runs sharing the same event sequence,
     * event source id and options - each run is committed as a single atomic `appendMany` call,
     * preserving the overall staging order across groups.
     */
    private suspend fun appendStagedEventsGroupedByStream(): List<AppendResult> {
        val results = mutableListOf<AppendResult>()
        var index = 0
        while (index < _stagedEvents.size) {
            val group = _stagedEvents[index]
            val batch = mutableListOf<Any>()
            while (index < _stagedEvents.size &&
                _stagedEvents[index].eventSequenceId == group.eventSequenceId &&
                _stagedEvents[index].eventSourceId == group.eventSourceId &&
                _stagedEvents[index].options == group.options
            ) {
                batch.add(_stagedEvents[index].event)
                index++
            }
            val sequence = eventStore.getEventSequence(group.eventSequenceId)
            results.addAll(sequence.appendMany(group.eventSourceId, batch, group.options))
        }
        return results
    }
}
