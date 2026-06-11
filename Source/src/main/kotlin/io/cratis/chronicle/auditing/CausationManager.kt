// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.auditing

import java.time.Instant

/**
 * Manages the causation chain for the current thread.
 *
 * Uses a [ThreadLocal] to scope the causation chain to each thread independently.
 */
class CausationManager {
    private val threadLocal: ThreadLocal<MutableList<Causation>> =
        ThreadLocal.withInitial { mutableListOf(Causation(Instant.now(), CausationType.root)) }

    /** Returns the current causation chain for this thread (read-only view). */
    val currentChain: List<Causation>
        get() = threadLocal.get().toList()

    /**
     * Adds a new [Causation] entry to the current thread's chain.
     *
     * @param type The [CausationType] for this entry.
     * @param properties Optional key/value properties associated with this entry.
     */
    fun add(type: CausationType, properties: Map<String, String> = emptyMap()) {
        threadLocal.get().add(Causation(Instant.now(), type, properties))
    }

    /**
     * Redefines the root causation (first entry) of the current thread's chain.
     *
     * @param properties Properties to associate with the root causation.
     */
    fun defineRoot(properties: Map<String, String> = emptyMap()) {
        val chain = threadLocal.get()
        val root = Causation(Instant.now(), CausationType.root, properties)
        if (chain.isEmpty()) {
            chain.add(root)
        } else {
            chain[0] = root
        }
    }

    /** Clears the causation chain for the current thread and re-seeds it with a root entry. */
    fun clear() {
        val chain = threadLocal.get()
        chain.clear()
        chain.add(Causation(Instant.now(), CausationType.root))
    }
}

/** Module-level singleton [CausationManager] used throughout the client. */
val causationManager: CausationManager = CausationManager()
