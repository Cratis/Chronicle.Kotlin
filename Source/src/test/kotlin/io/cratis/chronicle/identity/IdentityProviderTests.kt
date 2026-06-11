// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class IdentityProviderTests {

    private val provider = IdentityProvider()

    @BeforeEach
    fun reset() {
        provider.clearCurrentIdentity()
    }

    @Test
    fun `default identity is system`() {
        assertEquals(Identity.system.subject, provider.currentIdentity.subject)
    }

    @Test
    fun `setCurrentIdentity changes the current identity`() {
        val alice = Identity("u001", "Alice", "alice")
        provider.setCurrentIdentity(alice)
        assertEquals(alice, provider.currentIdentity)
    }

    @Test
    fun `clearCurrentIdentity resets to system`() {
        provider.setCurrentIdentity(Identity("u001", "Alice", "alice"))
        provider.clearCurrentIdentity()
        assertEquals(Identity.system.subject, provider.currentIdentity.subject)
    }

    @Test
    fun `identities are independent per thread`() {
        val alice = Identity("u001", "Alice", "alice")
        provider.setCurrentIdentity(alice)

        var threadIdentity: Identity? = null
        val thread = Thread {
            threadIdentity = provider.currentIdentity
        }
        thread.start()
        thread.join()

        assertEquals(Identity.system.subject, threadIdentity?.subject)
        assertEquals(alice, provider.currentIdentity)
    }
}
