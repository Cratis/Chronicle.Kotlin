// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

class UnitOfWorkManager {
    private val _active = ThreadLocal<UnitOfWork?>()

    val current: UnitOfWork? get() = _active.get()

    fun begin(): UnitOfWork {
        val uow = UnitOfWork()
        _active.set(uow)
        return uow
    }

    fun clear() {
        _active.set(null)
    }
}
