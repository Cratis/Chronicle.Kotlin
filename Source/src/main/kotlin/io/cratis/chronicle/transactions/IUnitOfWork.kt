// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.correlation.CorrelationId
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation

/**
 * Represents a unit of work - a set of event appends that are staged and then committed or rolled
 * back together.
 */
interface IUnitOfWork {
    /**
     * Gets a value indicating whether the unit of work is completed - either [commit]ed or
     * [rollback]ed.
     */
    val isCompleted: Boolean

    /**
     * Gets the [CorrelationId] for this [IUnitOfWork].
     */
    val correlationId: CorrelationId

    /**
     * Gets a value indicating whether this [IUnitOfWork] was successful.
     *
     * Before [commit], this is `true` unless something has already recorded a failure. After
     * [commit], it reflects whether every staged event was appended without a constraint
     * violation, concurrency violation, or append error.
     */
    val isSuccess: Boolean

    /**
     * Add an event that has occurred to this [IUnitOfWork].
     *
     * @param eventSequenceId The [EventSequenceId] for the event.
     * @param eventSourceId The identifier of the event source the event is for.
     * @param event The event that has occurred.
     * @param options Optional [AppendOptions] to use when this event is committed.
     */
    fun addEvent(eventSequenceId: EventSequenceId, eventSourceId: String, event: Any, options: AppendOptions? = null)

    /**
     * Get the events that have been added to this [IUnitOfWork] so far.
     *
     * @return A collection of events.
     */
    fun getEvents(): List<Any>

    /**
     * Gets any [ConstraintViolation]s that occurred while committing this [IUnitOfWork].
     *
     * @return A collection of [ConstraintViolation].
     */
    fun getConstraintViolations(): List<ConstraintViolation>

    /**
     * Gets any [ConcurrencyViolation]s that occurred while committing this [IUnitOfWork].
     *
     * @return A collection of [ConcurrencyViolation].
     */
    fun getConcurrencyViolations(): List<ConcurrencyViolation>

    /**
     * Gets any [AppendError]s that occurred while attempting to commit this [IUnitOfWork].
     *
     * @return A collection of [AppendError].
     */
    fun getAppendErrors(): List<AppendError>

    /**
     * Commit this [IUnitOfWork], appending all staged events.
     */
    suspend fun commit()

    /**
     * Rollback this [IUnitOfWork], discarding all staged events.
     */
    suspend fun rollback()

    /**
     * Register a callback to be called when this [IUnitOfWork] completes, whether by [commit] or
     * [rollback]. Can be called multiple times to register multiple callbacks.
     *
     * @param callback The callback to call.
     */
    fun onCompleted(callback: (IUnitOfWork) -> Unit)

    /**
     * Try to get the [EventSequenceNumber] of the last committed event.
     *
     * @return The [EventSequenceNumber] of the last committed event, or `null` if no events have
     *   been committed.
     */
    fun tryGetLastCommittedEventSequenceNumber(): EventSequenceNumber?
}
