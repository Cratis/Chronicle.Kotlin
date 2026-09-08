// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation
import java.util.UUID

/**
 * An explicit atomic transaction bound to one [eventSequence] and one [context].
 *
 * Events may span event-source identifiers, but cannot cross the sequence boundary. A successful
 * [commit] sends the complete ordered set in exactly one append-many request.
 */
interface IUnitOfWork {
    /** The only event sequence this transaction can append to. */
    val eventSequence: IEventSequence

    /** The immutable metadata used for the entire atomic batch. */
    val context: OperationContext

    /** Correlation identifier convenience for Java and existing result inspection. */
    val correlationId: UUID get() = context.correlationId

    /** Whether the unit of work is terminal: committed, rolled back, or failed. */
    val isCompleted: Boolean

    /** Whether a completed commit succeeded without violations, errors, or an exception. */
    val isSuccess: Boolean

    /** Stages one event in call order. */
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null)

    /** Stages multiple events in call order. */
    fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null)

    /** Returns staged event payloads in commit order. */
    fun getEvents(): List<Any>

    /** Returns all constraint violations from the commit. */
    fun getConstraintViolations(): List<ConstraintViolation>

    /** Returns every concurrency violation from the commit. */
    fun getConcurrencyViolations(): List<ConcurrencyViolation>

    /** Returns all append errors from the commit. */
    fun getAppendErrors(): List<AppendError>

    /**
     * Commits all staged events in one atomic append-many request.
     *
     * Completion callbacks run only after the state becomes terminal. Their failures are isolated
     * until every callback has run and are then reported as [UnitOfWorkCompletionCallbackException].
     * A successful append remains committed and is never retried when a callback fails.
     */
    suspend fun commit()

    /** Discards all staged events. */
    suspend fun rollback()

    /**
     * Registers a callback invoked once when the transaction becomes terminal.
     *
     * Registration while commit is in flight queues the callback. Registration after termination
     * invokes it immediately. Callback failures never prevent later registered callbacks from running.
     */
    fun onCompleted(callback: (IUnitOfWork) -> Unit)

    /** Returns the greatest sequence number assigned by a successful commit. */
    fun tryGetLastCommittedEventSequenceNumber(): EventSequenceNumber?
}
