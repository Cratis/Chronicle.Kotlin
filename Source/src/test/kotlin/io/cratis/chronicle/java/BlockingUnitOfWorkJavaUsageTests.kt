// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation
import io.cratis.chronicle.transactions.IUnitOfWork
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockingUnitOfWorkJavaUsageTests {
    @Test
    fun `ordinary Java can inspect every unit of work result without unwrap`() {
        val constraint = ConstraintViolation("unique", "already exists")
        val concurrency = ConcurrencyViolation("source", EventSequenceNumber(2), EventSequenceNumber(3))
        val appendError = AppendError("transport")
        val staged = listOf<Any>("first", "second")
        val unitOfWork = mockk<IUnitOfWork>()
        every { unitOfWork.getConstraintViolations() } returns listOf(constraint)
        every { unitOfWork.getConcurrencyViolations() } returns listOf(concurrency)
        every { unitOfWork.getAppendErrors() } returns listOf(appendError)
        every { unitOfWork.getEvents() } returns staged
        every { unitOfWork.tryGetLastCommittedEventSequenceNumber() } returns EventSequenceNumber(42)

        val snapshot = BlockingUnitOfWorkJavaUsage.inspect(BlockingUnitOfWork(unitOfWork))

        assertEquals(listOf(constraint), snapshot.constraintViolations())
        assertEquals(listOf(concurrency), snapshot.concurrencyViolations())
        assertEquals(listOf(appendError), snapshot.appendErrors())
        assertEquals(staged, snapshot.stagedEvents())
        assertEquals(42L, snapshot.lastCommittedSequenceNumber())
    }
}
