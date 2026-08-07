// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceId
import kotlinx.coroutines.runBlocking

/**
 * An event store for Java, without the coroutines.
 *
 * @param store The event store to forward to.
 */
class BlockingEventStore(private val store: IEventStore) {

    /** The suspending event store underneath, for the services this does not wrap. */
    fun unwrap(): IEventStore = store

    /** The name of the event store. */
    val name: String get() = store.name

    /** The namespace within it. */
    val namespace: String get() = store.namespace

    /** The default event log. */
    val eventLog: BlockingEventSequence by lazy { BlockingEventSequence(store.eventLog) }

    /** The transactional view of the event log, staging appends against the current unit of work. */
    val transactional: BlockingTransactionalEventSequence by lazy {
        BlockingTransactionalEventSequence(store.eventLog.transactional)
    }

    /** The read models, taking a plain `Class` rather than a Kotlin `KClass`. */
    val readModels: BlockingReadModels by lazy { BlockingReadModels(store.readModels) }

    /** The reactors, for registering one by hand. */
    val reactors: BlockingReactors by lazy { BlockingReactors(store.reactors) }

    /** The reducers, for registering one by hand. */
    val reducers: BlockingReducers by lazy { BlockingReducers(store.reducers) }

    /** Begins a unit of work, so several appends commit as one atomic operation. */
    fun beginUnitOfWork(): BlockingUnitOfWork = BlockingUnitOfWork(store.unitOfWorkManager.begin())

    /** Any other event sequence, by id. */
    fun getEventSequence(id: String): BlockingEventSequence =
        BlockingEventSequence(store.getEventSequence(EventSequenceId(id)))

    /**
     * Registers every discovered artifact with the kernel now.
     *
     * Rarely needed: discovery does this on connect. Reach for it when registration was turned off
     * and you want to control when it happens.
     */
    fun registerAll() {
        runBlocking { store.registerAll() }
    }

    /**
     * Waits for the first registration pass to finish.
     *
     * Also rarely needed - an append waits for it on your behalf. This is for the case where you
     * want to know registration is done before doing something that is not an append.
     */
    fun awaitRegistration() {
        runBlocking { store.awaitRegistration() }
    }
}
