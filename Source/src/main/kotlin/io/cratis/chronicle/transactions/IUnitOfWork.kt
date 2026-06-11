// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

interface IUnitOfWork {
    suspend fun commit()
    suspend fun rollback()
}
