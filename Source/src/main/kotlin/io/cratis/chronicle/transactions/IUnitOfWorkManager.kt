// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.correlation.CorrelationId

/**
 * Defines a system that can manage [IUnitOfWork] instances.
 */
interface IUnitOfWorkManager {
    /**
     * Gets the current [IUnitOfWork], scoped to the current thread.
     *
     * @throws NoUnitOfWorkHasBeenStarted If no unit of work has been started.
     */
    val current: IUnitOfWork

    /**
     * Gets a value indicating whether there is a current [IUnitOfWork].
     */
    val hasCurrent: Boolean

    /**
     * Try to get the [IUnitOfWork] for a specific [CorrelationId].
     *
     * @param correlationId The [CorrelationId] to get the [IUnitOfWork] for.
     * @return The [IUnitOfWork] if it was found, otherwise `null`.
     */
    fun tryGetFor(correlationId: CorrelationId): IUnitOfWork?

    /**
     * Begin a new [IUnitOfWork] with an auto-generated [CorrelationId] for the current thread.
     *
     * @return A new [IUnitOfWork].
     */
    fun begin(): IUnitOfWork

    /**
     * Begin a new [IUnitOfWork] with a specific [CorrelationId] for the current thread.
     *
     * @param correlationId The [CorrelationId] to use for the [IUnitOfWork].
     * @return A new [IUnitOfWork].
     */
    fun begin(correlationId: CorrelationId): IUnitOfWork

    /**
     * Set the current [IUnitOfWork].
     *
     * @param unitOfWork The [IUnitOfWork] to set as current.
     */
    fun setCurrent(unitOfWork: IUnitOfWork)
}
