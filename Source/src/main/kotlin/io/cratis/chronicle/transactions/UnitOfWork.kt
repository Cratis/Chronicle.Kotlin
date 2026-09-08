// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation

/**
 * Explicit [IUnitOfWork] implementation bound to one [eventSequence] and one [context].
 */
class UnitOfWork @JvmOverloads constructor(
    override val eventSequence: IEventSequence,
    override val context: OperationContext = OperationContext.system()
) : IUnitOfWork {
    private data class StagedEvent(
        val eventSourceId: String,
        val event: Any,
        val options: AppendOptions?
    )

    private enum class State { OPEN, COMMITTING, COMMITTED, ROLLED_BACK, FAILED }

    private val stagedEvents = mutableListOf<StagedEvent>()
    private val completionCallbacks = mutableListOf<(IUnitOfWork) -> Unit>()
    private var completionCallbacksDelivered: Boolean = false
    private var appendResults: List<AppendResult> = emptyList()
    private var lastCommittedEventSequenceNumber: EventSequenceNumber? = null
    private var state: State = State.OPEN

    override val isCompleted: Boolean
        get() = synchronized(this) { state == State.COMMITTED || state == State.ROLLED_BACK || state == State.FAILED }

    override val isSuccess: Boolean
        get() = synchronized(this) {
            state == State.COMMITTED && appendResults.all { it.isSuccess }
        }

    override fun append(eventSourceId: String, event: Any, options: AppendOptions?) {
        synchronized(this) {
            requireOpen()
            stagedEvents.add(StagedEvent(eventSourceId, event, options))
        }
    }

    override fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions?) {
        synchronized(this) {
            requireOpen()
            events.forEach { stagedEvents.add(StagedEvent(eventSourceId, it, options)) }
        }
    }

    override fun getEvents(): List<Any> = synchronized(this) { stagedEvents.map { it.event } }

    override fun getConstraintViolations(): List<ConstraintViolation> =
        synchronized(this) { appendResults.flatMap { it.constraintViolations }.distinct() }

    override fun getConcurrencyViolations(): List<ConcurrencyViolation> =
        synchronized(this) { appendResults.flatMap { it.concurrencyViolations }.distinct() }

    override fun getAppendErrors(): List<AppendError> =
        synchronized(this) { appendResults.flatMap { it.errors }.distinct() }

    override suspend fun commit() {
        val snapshot = synchronized(this) {
            requireOpen()
            state = State.COMMITTING
            stagedEvents.toList()
        }

        var appendFailure: Throwable? = null
        try {
            val results = if (snapshot.isEmpty()) {
                emptyList()
            } else {
                eventSequence.appendMany(
                    events = snapshot.map { it.toEvent() },
                    context = context,
                    concurrencyScopes = concurrencyScopesFor(snapshot)
                )
            }
            synchronized(this) {
                appendResults = results
                lastCommittedEventSequenceNumber = results
                    .asSequence()
                    .map { it.sequenceNumber }
                    .filter { it.isActualValue }
                    .maxByOrNull { it.value }
                state = State.COMMITTED
            }
        } catch (throwable: Throwable) {
            synchronized(this) { state = State.FAILED }
            appendFailure = throwable
        }

        val callbackFailure = notifyCompleted()
        appendFailure?.let { failure ->
            callbackFailure?.let(failure::addSuppressed)
            throw failure
        }
        callbackFailure?.let { throw it }
    }

    override suspend fun rollback() {
        synchronized(this) {
            requireOpen()
            state = State.ROLLED_BACK
            stagedEvents.clear()
            appendResults = emptyList()
        }
        notifyCompleted()?.let { throw it }
    }

    override fun onCompleted(callback: (IUnitOfWork) -> Unit) {
        val invokeImmediately = synchronized(this) {
            if (state == State.OPEN || state == State.COMMITTING || !completionCallbacksDelivered) {
                completionCallbacks.add(callback)
                false
            } else {
                true
            }
        }
        if (invokeImmediately) {
            invokeCompletionCallbacks(listOf(callback))?.let { throw it }
        }
    }

    override fun tryGetLastCommittedEventSequenceNumber(): EventSequenceNumber? =
        synchronized(this) { lastCommittedEventSequenceNumber }

    private fun requireOpen() {
        check(state == State.OPEN) { "The unit of work is already terminal ($state)" }
    }

    private fun notifyCompleted(): UnitOfWorkCompletionCallbackException? {
        val failures = mutableListOf<Throwable>()
        while (true) {
            val callbacks = synchronized(this) {
                if (completionCallbacks.isEmpty()) {
                    completionCallbacksDelivered = true
                    emptyList()
                } else {
                    completionCallbacks.toList().also { completionCallbacks.clear() }
                }
            }
            if (callbacks.isEmpty()) break
            invokeCompletionCallbacks(callbacks)?.let { failures.addAll(it.failures) }
        }
        return failures.takeIf { it.isNotEmpty() }?.let(::UnitOfWorkCompletionCallbackException)
    }

    private fun invokeCompletionCallbacks(
        callbacks: List<(IUnitOfWork) -> Unit>
    ): UnitOfWorkCompletionCallbackException? {
        val failures = callbacks.mapNotNull { callback ->
            try {
                callback(this)
                null
            } catch (throwable: Throwable) {
                throwable
            }
        }
        return failures.takeIf { it.isNotEmpty() }?.let(::UnitOfWorkCompletionCallbackException)
    }

    private fun StagedEvent.toEvent(): EventForEventSourceId = EventForEventSourceId(
        eventSourceId = eventSourceId,
        event = event,
        eventStreamType = options?.eventStreamType,
        eventStreamId = options?.eventStreamId,
        eventSourceType = options?.eventSourceType,
        tags = options?.tags ?: emptyList(),
        occurred = options?.occurred,
        subject = options?.subject
    )

    private fun concurrencyScopesFor(events: List<StagedEvent>): Map<String, ConcurrencyScope> {
        val scopes = linkedMapOf<String, ConcurrencyScope>()
        events.forEach { staged ->
            val scope = staged.options?.concurrencyScope ?: return@forEach
            val previous = scopes.putIfAbsent(staged.eventSourceId, scope)
            require(previous == null || previous == scope) {
                "Event source '${staged.eventSourceId}' has conflicting concurrency scopes in one unit of work"
            }
        }
        return scopes
    }
}

/** Starts an explicit transaction bound to this event sequence. */
fun IEventSequence.beginUnitOfWork(context: OperationContext = OperationContext.system()): UnitOfWork =
    UnitOfWork(this, context)
