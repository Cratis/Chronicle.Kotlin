// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import kotlin.reflect.KClass

/**
 * The partitions an observer is currently failing on, and how to get it moving again.
 *
 * A handler that throws stops the event source it threw on and leaves every other one running. That
 * is deliberate - one bad event should not halt the system - but it also means a stuck partition
 * announces itself nowhere. This is how an application finds out, and how an operator recovers once
 * the cause is fixed.
 *
 * ```kotlin
 * for (partition in store.failedPartitions.getFor(EmployeeAlerts::class)) {
 *     println("${partition.partition}: ${partition.lastAttempt?.messages?.joinToString()}")
 *     store.failedPartitions.retry(EmployeeAlerts::class, partition.partition)
 * }
 * ```
 */
interface IFailedPartitions {
    /**
     * The partitions [observerId] is currently failing on, empty when it is healthy.
     *
     * @param observerId The observer to ask about.
     * @return Every failing partition, each with the history of attempts made on it.
     */
    suspend fun getFor(observerId: String): List<FailedPartition>

    /**
     * The partitions the observer declared by [observerClass] is currently failing on.
     *
     * The identifier is read off the class the same way registration reads it, so a reactor or
     * reducer can be asked about by type rather than by remembering what its id came out as.
     *
     * @param observerClass The reactor or reducer class.
     * @return Every failing partition, each with the history of attempts made on it.
     */
    suspend fun getFor(observerClass: KClass<*>): List<FailedPartition>

    /**
     * Asks the kernel to try [partition] again.
     *
     * Retrying an observer whose cause has not been fixed simply adds another attempt, so fix first
     * and retry after.
     *
     * @param observerId The observer that is failing.
     * @param partition The event source to try again.
     * @param eventSequenceId The sequence the observer is on. Defaults to the event log.
     */
    suspend fun retry(
        observerId: String,
        partition: String,
        eventSequenceId: EventSequenceId = EventSequenceId.eventLog
    )

    /**
     * Asks the kernel to try [partition] again for the observer declared by [observerClass].
     *
     * The sequence is read off the class, so an observer watching something other than the event log
     * is retried on the sequence it actually observes.
     *
     * @param observerClass The reactor or reducer class.
     * @param partition The event source to try again.
     */
    suspend fun retry(observerClass: KClass<*>, partition: String)
}
