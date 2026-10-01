// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.FailedPartitionsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Observation
import Cratis.Chronicle.Contracts.Observation.ObserversGrpcKt
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.toUuid
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Talks to the kernel about the partitions observers are stuck on.
 *
 * @param eventStoreName The event store the observers belong to.
 * @param namespace The namespace within the event store.
 * @param failedPartitions The stub failing partitions are read through.
 * @param observers The stub retries are asked for through.
 */
class FailedPartitions(
    private val eventStoreName: String,
    private val namespace: String,
    private val failedPartitions: FailedPartitionsGrpcKt.FailedPartitionsCoroutineStub,
    private val observers: ObserversGrpcKt.ObserversCoroutineStub
) : IFailedPartitions {

    override suspend fun getFor(observerId: String): List<FailedPartition> {
        val request = Observation.GetFailedPartitionsRequest.newBuilder().apply {
            this.eventStore = eventStoreName
            this.namespace = this@FailedPartitions.namespace
            this.observerId = observerId
        }.build()

        return failedPartitions.getFailedPartitions(request).itemsList.map { it.toClient() }
    }

    override suspend fun getFor(observerClass: KClass<*>): List<FailedPartition> =
        getFor(ObserverDeclaration.idOf(observerClass))

    override suspend fun retry(observerId: String, partition: String, eventSequenceId: EventSequenceId) {
        val request = Observation.RetryPartition.newBuilder().apply {
            this.eventStore = eventStoreName
            this.namespace = this@FailedPartitions.namespace
            this.observerId = observerId
            this.eventSequenceId = eventSequenceId.value
            this.partition = partition
        }.build()

        observers.retryPartition(request)
    }

    override suspend fun retry(observerClass: KClass<*>, partition: String) = retry(
        observerId = ObserverDeclaration.idOf(observerClass),
        partition = partition,
        eventSequenceId = EventSequenceId(ObserverDeclaration.eventSequenceIdOf(observerClass))
    )

    private fun Observation.FailedPartition.toClient() = FailedPartition(
        id = id.toUuid(),
        observerId = observerId,
        partition = partition,
        attempts = attemptsList.map { it.toClient() }
    )

    private fun Observation.FailedPartitionAttempt.toClient() = FailedPartitionAttempt(
        // An attempt the kernel never stamped comes through as an empty string rather than as an
        // absent field, so parsing has to tolerate one instead of throwing on it.
        occurred = runCatching { Instant.parse(occurred.value) }.getOrDefault(Instant.EPOCH),
        sequenceNumber = EventSequenceNumber(sequenceNumber),
        messages = messagesList.toList(),
        stackTrace = stackTrace
    )
}

