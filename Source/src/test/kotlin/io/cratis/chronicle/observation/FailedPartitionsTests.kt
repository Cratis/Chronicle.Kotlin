// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.FailedPartitionsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Observation
import Cratis.Chronicle.Contracts.Observation.ObserversGrpcKt
import bcl.Bcl
import com.google.protobuf.Empty
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * A handler that throws stops the event source it threw on and nothing else, so a stuck partition
 * announces itself nowhere. These pin how it is found and retried.
 */
class FailedPartitionsTests {

    @Reactor(id = "employee-alerts")
    private class EmployeeAlerts {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @EventSequence("audit")
    private class AuditTrail {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    private val partitionId = UUID.fromString("6f1a9b6a-0f6d-4f3f-9b8e-9a2f0d5e1c77")

    private fun UUID.toContract(): Bcl.Guid = Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
        .build()

    private fun onePartition() = Observation.IEnumerable_FailedPartition.newBuilder()
        .addItems(
            Observation.FailedPartition.newBuilder()
                .setId(partitionId.toContract())
                .setObserverId("employee-alerts")
                .setPartition("employee-1")
                .addAttempts(
                    Observation.FailedPartitionAttempt.newBuilder()
                        .setOccurred(
                            Observation.SerializableDateTimeOffset.newBuilder()
                                .setValue("2026-08-06T10:15:30Z")
                        )
                        .setSequenceNumber(7)
                        .addMessages("mail server refused the connection")
                        .setStackTrace("at Mailer.send")
                )
                .addAttempts(
                    Observation.FailedPartitionAttempt.newBuilder()
                        .setOccurred(
                            Observation.SerializableDateTimeOffset.newBuilder()
                                .setValue("2026-08-06T10:20:30Z")
                        )
                        .setSequenceNumber(7)
                        .addMessages("mail server still refusing")
                        .setStackTrace("at Mailer.send")
                )
        )
        .build()

    private fun failedPartitionsFor(
        response: Observation.IEnumerable_FailedPartition,
        request: CapturingSlot<Observation.GetFailedPartitionsRequest>? = null,
        observers: ObserversGrpcKt.ObserversCoroutineStub = mockk()
    ): FailedPartitions {
        val stub = mockk<FailedPartitionsGrpcKt.FailedPartitionsCoroutineStub>()
        if (request != null) {
            coEvery { stub.getFailedPartitions(capture(request), any()) } returns response
        } else {
            coEvery { stub.getFailedPartitions(any(), any()) } returns response
        }
        return FailedPartitions("my-store", "default", stub, observers)
    }

    @Test
    fun `a failing partition comes back with the history of what went wrong`() = runBlocking {
        val partitions = failedPartitionsFor(onePartition()).getFor("employee-alerts")

        val partition = partitions.single()
        assertEquals(partitionId, partition.id)
        assertEquals("employee-alerts", partition.observerId)
        assertEquals("employee-1", partition.partition)
        assertEquals(2, partition.attempts.size)
        assertEquals(7L, partition.attempts.first().sequenceNumber.value)
        assertEquals(listOf("mail server refused the connection"), partition.attempts.first().messages)
        assertEquals(Instant.parse("2026-08-06T10:15:30Z"), partition.attempts.first().occurred)
    }

    @Test
    fun `the last attempt is the one that explains why it is still stuck`() = runBlocking {
        val partition = failedPartitionsFor(onePartition()).getFor("employee-alerts").single()
        assertEquals(listOf("mail server still refusing"), partition.lastAttempt!!.messages)
    }

    @Test
    fun `a healthy observer has no failing partitions`() = runBlocking {
        val empty = Observation.IEnumerable_FailedPartition.newBuilder().build()
        assertTrue(failedPartitionsFor(empty).getFor("employee-alerts").isEmpty())
    }

    @Test
    fun `a partition with no attempts at all has no last attempt`() = runBlocking {
        val response = Observation.IEnumerable_FailedPartition.newBuilder()
            .addItems(
                Observation.FailedPartition.newBuilder()
                    .setId(partitionId.toContract())
                    .setObserverId("employee-alerts")
                    .setPartition("employee-1")
            )
            .build()

        assertNull(failedPartitionsFor(response).getFor("employee-alerts").single().lastAttempt)
    }

    @Test
    fun `asking by type uses the id the reactor registers under`() = runBlocking {
        val request = slot<Observation.GetFailedPartitionsRequest>()
        failedPartitionsFor(onePartition(), request).getFor(EmployeeAlerts::class)

        assertEquals("employee-alerts", request.captured.observerId)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
    }

    @Test
    fun `retrying names the observer, the partition and the sequence`() = runBlocking {
        val retry = slot<Observation.RetryPartition>()
        val observers = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { observers.retryPartition(capture(retry), any()) } returns Empty.getDefaultInstance()

        failedPartitionsFor(onePartition(), observers = observers)
            .retry("employee-alerts", "employee-1", EventSequenceId.eventLog)

        assertEquals("employee-alerts", retry.captured.observerId)
        assertEquals("employee-1", retry.captured.partition)
        assertEquals(EventSequenceId.eventLog.value, retry.captured.eventSequenceId)
    }

    @Test
    fun `retrying by type retries on the sequence that observer actually watches`() = runBlocking {
        val retry = slot<Observation.RetryPartition>()
        val observers = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { observers.retryPartition(capture(retry), any()) } returns Empty.getDefaultInstance()

        failedPartitionsFor(onePartition(), observers = observers).retry(AuditTrail::class, "employee-1")

        assertEquals("AuditTrail", retry.captured.observerId)
        assertEquals("audit", retry.captured.eventSequenceId)
    }
}
