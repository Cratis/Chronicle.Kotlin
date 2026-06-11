// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import java.util.UUID

/**
 * Manages the current correlation identifier for each thread.
 *
 * Uses a [ThreadLocal] to scope the correlation ID to each thread independently.
 */
class CorrelationIdManager {
    private val threadLocal: ThreadLocal<UUID?> = ThreadLocal.withInitial<UUID?> { null }

    /**
     * Returns the current [UUID] correlation identifier for this thread.
     * If none has been set, a new one is created automatically.
     */
    val current: UUID
        get() = threadLocal.get() ?: UUID.randomUUID().also { threadLocal.set(it) }

    /**
     * Sets the correlation identifier for the current thread.
     *
     * @param id The [UUID] to use as the current correlation identifier.
     */
    fun set(id: UUID) {
        threadLocal.set(id)
    }

    /** Clears the correlation identifier for the current thread; next access will generate a new one. */
    fun clear() {
        threadLocal.remove()
    }
}

/** Module-level singleton [CorrelationIdManager] used throughout the client. */
val correlationIdManager: CorrelationIdManager = CorrelationIdManager()
