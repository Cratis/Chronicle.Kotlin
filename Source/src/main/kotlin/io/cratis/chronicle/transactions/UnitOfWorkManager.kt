// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.correlation.CorrelationId
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents an implementation of [IUnitOfWorkManager].
 *
 * @param eventStore The [IEventStore] to use for [IUnitOfWork] instances created by this manager.
 */
class UnitOfWorkManager(private val eventStore: IEventStore) : IUnitOfWorkManager {
    private val _active = ThreadLocal<UnitOfWork?>()
    private val _unitsOfWork = ConcurrentHashMap<CorrelationId, IUnitOfWork>()

    override val current: UnitOfWork
        get() = _active.get() ?: throw NoUnitOfWorkHasBeenStarted()

    override val hasCurrent: Boolean get() = _active.get() != null

    override fun tryGetFor(correlationId: CorrelationId): IUnitOfWork? = _unitsOfWork[correlationId]

    override fun begin(): UnitOfWork = begin(CorrelationId.create())

    override fun begin(correlationId: CorrelationId): UnitOfWork {
        val unitOfWork = UnitOfWork(correlationId, eventStore, ::onUnitOfWorkCompleted)
        _active.set(unitOfWork)
        _unitsOfWork[correlationId] = unitOfWork
        return unitOfWork
    }

    override fun setCurrent(unitOfWork: IUnitOfWork) {
        if (unitOfWork !is UnitOfWork) {
            throw IllegalArgumentException("setCurrent only supports instances of UnitOfWork")
        }
        _active.set(unitOfWork)
        _unitsOfWork[unitOfWork.correlationId] = unitOfWork
        unitOfWork.onCompleted(::onUnitOfWorkCompleted)
    }

    /** Clears the current unit of work for this thread without completing it. Mainly useful for tests. */
    fun clear() {
        _active.set(null)
    }

    private fun onUnitOfWorkCompleted(unitOfWork: IUnitOfWork) {
        _unitsOfWork.remove(unitOfWork.correlationId)
        if (_active.get() === unitOfWork) {
            _active.set(null)
        }
    }
}
