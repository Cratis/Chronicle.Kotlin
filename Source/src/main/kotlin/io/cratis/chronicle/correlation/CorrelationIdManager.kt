// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import io.cratis.fundamentals.execution.CorrelationId
import io.cratis.fundamentals.execution.CorrelationIdContext
import java.util.UUID

/**
 * Manages the current correlation identifier for each thread.
 *
 * Delegates to a thread-bound [CorrelationIdContext], scoping the correlation ID to each thread independently.
 * The module-level [correlationIdManager] is bound to [CorrelationIdContext.shared]; managers created with the
 * constructor own an independent context.
 */
class CorrelationIdManager internal constructor(private val context: CorrelationIdContext) {
    /** Creates a manager with its own independent, thread-bound state. */
    constructor() : this(CorrelationIdContext())

    /**
     * Returns the current [UUID] correlation identifier for this thread.
     * If none has been set, a new one is created automatically.
     */
    val current: UUID
        get() = context.current.value()

    /**
     * Sets the correlation identifier for the current thread.
     *
     * @param id The [UUID] to use as the current correlation identifier.
     */
    fun set(id: UUID) {
        context.set(CorrelationId(id))
    }

    /** Clears the correlation identifier for the current thread; next access will generate a new one. */
    fun clear() {
        context.clear()
    }
}

/** Module-level singleton [CorrelationIdManager] used throughout the client. */
val correlationIdManager: CorrelationIdManager = CorrelationIdManager(CorrelationIdContext.shared())
