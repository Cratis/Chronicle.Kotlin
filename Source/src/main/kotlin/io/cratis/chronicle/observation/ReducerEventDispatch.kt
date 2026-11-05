// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.json.chronicleGson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Duration

/**
 * Runs one delivery of events past a reducer, folding each event source of the page from its stored
 * state, and reports one outcome per partition.
 *
 * The event sources of a page are independent folds, so they run at the same time, up to the
 * reducer's [ReducerRegistration.maxConcurrency]. [ReducersService] is left holding the gRPC
 * conversation.
 *
 * @param registration What was read off the reducer class at registration.
 * @param fold Folds one event source's events.
 */
internal class ReducerEventDispatch(
    private val registration: ReducerRegistration,
    private val fold: ReducerFold
) {
    /** Folds every partition of [page], answering in the page's order. */
    suspend fun observe(page: DeliveredPage, context: BatchContext): List<PartitionOutcome> {
        val permits = Semaphore(registration.maxConcurrency.coerceAtLeast(1))
        return coroutineScope {
            page.partitions.map { partition ->
                async { permits.withPermit { reduce(partition, context) } }
            }.awaitAll()
        }
    }

    private suspend fun reduce(partition: DeliveredPartition, context: BatchContext): PartitionOutcome {
        return when (val result = fold.reduce(partition.events, initialStateOf(partition), context)) {
            is ReduceResult.Reduced -> PartitionOutcome(
                partition.partition,
                PartitionState.Success,
                result.lastObserved,
                readModelState = result.state?.let { chronicleGson.toJson(it) }
            )

            is ReduceResult.Failed -> PartitionOutcome.failed(partition.partition, result.error, result.methodName)

            is ReduceResult.Deferred -> PartitionOutcome(
                partition.partition,
                PartitionState.Deferred,
                PartitionOutcome.NONE_OBSERVED,
                retryAfter = result.retryAfter.takeUnless { it.isNegative } ?: Duration.ZERO
            )
        }
    }

    /**
     * The stored read model, or `null` when there is none - or when it no longer reads as the read
     * model class, which folds the event source's events from scratch as the client always has.
     */
    private fun initialStateOf(partition: DeliveredPartition): Any? {
        val readModelClass = registration.readModelClass ?: return null
        if (partition.initialState.isBlank()) return null
        return try {
            chronicleGson.fromJson(partition.initialState, readModelClass.java)
        } catch (_: Exception) {
            null
        }
    }
}
