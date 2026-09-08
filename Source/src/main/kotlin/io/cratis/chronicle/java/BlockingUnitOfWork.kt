// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation
import io.cratis.chronicle.transactions.IUnitOfWork
import kotlinx.coroutines.runBlocking

/** Blocking, ordinary-Java view of an explicit unit of work. */
class BlockingUnitOfWork(private val unitOfWork: IUnitOfWork) : AutoCloseable {
    /** The suspending unit of work underneath. */
    fun unwrap(): IUnitOfWork = unitOfWork

    /** Whether the unit of work is terminal: committed, rolled back, or failed. */
    val isCompleted: Boolean get() = unitOfWork.isCompleted

    /** Whether the completed commit succeeded. */
    val isSuccess: Boolean get() = unitOfWork.isSuccess

    /** Correlation identifier for the atomic batch. */
    val correlationId: String get() = unitOfWork.correlationId.toString()

    /** Constraint violations returned by the completed commit. */
    val constraintViolations: List<ConstraintViolation> get() = unitOfWork.getConstraintViolations()

    /** Every concurrency violation returned by the completed commit. */
    val concurrencyViolations: List<ConcurrencyViolation> get() = unitOfWork.getConcurrencyViolations()

    /** Append errors returned by the completed commit. */
    val appendErrors: List<AppendError> get() = unitOfWork.getAppendErrors()

    /** Event payloads currently staged in commit order. */
    val stagedEvents: List<Any> get() = unitOfWork.getEvents()

    /** Greatest committed sequence number, or `null` when no event was committed. */
    val lastCommittedSequenceNumber: Long?
        get() = unitOfWork.tryGetLastCommittedEventSequenceNumber()?.value

    /** Stages one event. */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null) {
        unitOfWork.append(eventSourceId, event, options)
    }

    /** Stages events in order. */
    @JvmOverloads
    fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null) {
        unitOfWork.appendMany(eventSourceId, events, options)
    }

    /** Commits the complete ordered batch with one append-many RPC. */
    fun commit() {
        runBlocking { unitOfWork.commit() }
    }

    /** Discards the staged batch. */
    fun rollback() {
        runBlocking { unitOfWork.rollback() }
    }

    /** Rolls back an uncompleted transaction for try-with-resources. */
    override fun close() {
        if (!isCompleted) rollback()
    }
}
