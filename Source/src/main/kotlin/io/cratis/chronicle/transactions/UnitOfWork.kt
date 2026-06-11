// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import java.util.UUID

class UnitOfWork(val id: UUID = UUID.randomUUID()) : IUnitOfWork {
    private val _pendingEvents = mutableListOf<Pair<String, Any>>()
    internal val pendingEvents: List<Pair<String, Any>> get() = _pendingEvents.toList()

    var isCommitted: Boolean = false
        private set

    fun stage(eventSourceId: String, event: Any) {
        _pendingEvents.add(eventSourceId to event)
    }

    override suspend fun commit() {
        // Flush is handled by the commit callback set by UnitOfWorkManager
        commitCallback?.invoke(this)
        isCommitted = true
    }

    override suspend fun rollback() {
        _pendingEvents.clear()
    }

    internal var commitCallback: (suspend (UnitOfWork) -> Unit)? = null
}
