// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.transactions.IUnitOfWork
import kotlinx.coroutines.runBlocking

/**
 * A unit of work for Java, without the coroutines.
 *
 * @param unitOfWork The unit of work to forward to.
 */
class BlockingUnitOfWork(private val unitOfWork: IUnitOfWork) : AutoCloseable {

    /** The suspending unit of work underneath. */
    fun unwrap(): IUnitOfWork = unitOfWork

    /** Whether it has been committed or rolled back. */
    val isCompleted: Boolean get() = unitOfWork.isCompleted

    /** Whether it completed without a constraint violation or error. */
    val isSuccess: Boolean get() = unitOfWork.isSuccess

    /** The correlation identifier the whole unit of work is held under. */
    val correlationId: String get() = unitOfWork.correlationId.value.toString()

    /** Commits everything staged, appending it as one atomic operation. */
    fun commit() {
        runBlocking { unitOfWork.commit() }
    }

    /** Discards everything staged. */
    fun rollback() {
        runBlocking { unitOfWork.rollback() }
    }

    /**
     * Rolls back unless the unit of work was already completed, so try-with-resources does the right
     * thing on both paths:
     *
     * ```java
     * try (var unitOfWork = store.getUnitOfWorkManager().begin()) {
     *     eventLog.append("order-123", new OrderPlaced(...));
     *     unitOfWork.commit();
     * }   // committed above; nothing to undo. On a throw, rolled back here.
     * ```
     */
    override fun close() {
        if (!isCompleted) rollback()
    }
}
