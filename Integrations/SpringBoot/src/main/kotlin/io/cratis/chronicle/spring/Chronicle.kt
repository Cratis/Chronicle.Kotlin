// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.readModels.ReadModelSnapshot
import io.cratis.chronicle.transactions.IUnitOfWork
import io.cratis.chronicle.transactions.UnitOfWork
import kotlinx.coroutines.runBlocking

/** Blocking Spring-friendly access to everyday Chronicle operations. */
class Chronicle(val eventStore: IEventStore) {
    /** Appends with a fresh per-call system context. */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        runBlocking { eventStore.eventLog.append(eventSourceId, event, options) }

    /** Appends with explicit immutable operation metadata. */
    @JvmOverloads
    fun append(
        eventSourceId: String,
        event: Any,
        context: OperationContext,
        options: AppendOptions? = null
    ): AppendResult = runBlocking { eventStore.eventLog.append(eventSourceId, event, context, options) }

    /** Appends one-source events atomically with a fresh per-call system context. */
    @JvmOverloads
    fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult> =
        runBlocking { eventStore.eventLog.appendMany(eventSourceId, events, options) }

    /** Appends one-source events atomically with explicit immutable metadata. */
    @JvmOverloads
    fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        context: OperationContext,
        options: AppendOptions? = null
    ): List<AppendResult> = runBlocking { eventStore.eventLog.appendMany(eventSourceId, events, context, options) }

    /** Reads one read-model instance. */
    fun <T : Any> readModel(readModelType: Class<T>, key: String): T? =
        runBlocking { eventStore.readModels.getInstanceByKey(readModelType.kotlin, key) }

    /** Reads all instances of a read model. */
    fun <T : Any> readModels(readModelType: Class<T>): List<T> =
        runBlocking { eventStore.readModels.getInstances(readModelType.kotlin) }

    /** Reads historical snapshots of one read-model instance. */
    fun <T : Any> readModelHistory(readModelType: Class<T>, key: String): List<ReadModelSnapshot<T>> =
        runBlocking { eventStore.readModels.getSnapshotsById(readModelType.kotlin, key) }

    /**
     * Runs [work] against one explicit transaction bound to the current event log and [context].
     * Call [IUnitOfWork.append] from [work]; direct event-log appends never enroll implicitly.
     */
    @JvmOverloads
    fun <T> inUnitOfWork(
        context: OperationContext = OperationContext.system(),
        work: (IUnitOfWork) -> T
    ): IUnitOfWork {
        val unitOfWork = UnitOfWork(eventStore.eventLog, context)
        try {
            work(unitOfWork)
            if (!unitOfWork.isCompleted) runBlocking { unitOfWork.commit() }
        } catch (throwable: Throwable) {
            if (!unitOfWork.isCompleted) runBlocking { unitOfWork.rollback() }
            throw throwable
        }
        return unitOfWork
    }

    /** Registers all application artifacts. */
    fun registerAll() {
        runBlocking { eventStore.registerAll() }
    }
}
