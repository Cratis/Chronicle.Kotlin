// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identity

/**
 * Provides and manages the [Identity] for the current thread.
 *
 * Uses a [ThreadLocal] so that each thread maintains its own identity independently.
 */
class IdentityProvider {
    private val threadLocal = ThreadLocal.withInitial<Identity> { Identity.system }

    /** Returns the current [Identity] for this thread, defaulting to [Identity.system]. */
    val currentIdentity: Identity
        get() = threadLocal.get()

    /**
     * Sets the [Identity] for the current thread.
     *
     * @param identity The identity to associate with the current thread.
     */
    fun setCurrentIdentity(identity: Identity) {
        threadLocal.set(identity)
    }

    /** Resets the identity for the current thread back to [Identity.system]. */
    fun clearCurrentIdentity() {
        threadLocal.set(Identity.system)
    }
}
