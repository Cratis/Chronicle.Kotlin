// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Hands out configured fetch outcomes and counts how many were asked for. */
private class FakeTokenFetches(private val outcomes: List<() -> OAuthTokenEndpoint.Token>) {
    var count = 0
        private set

    fun fetch(): OAuthTokenEndpoint.Token {
        // The last configured outcome repeats for any further fetches.
        val outcome = outcomes[minOf(count, outcomes.size - 1)]
        count++
        return outcome()
    }
}

class OAuthTokenProviderTests {

    private var nowMillis = 0L

    private fun providerFetching(
        vararg outcomes: () -> OAuthTokenEndpoint.Token
    ): Pair<OAuthTokenProvider, FakeTokenFetches> {
        val fetches = FakeTokenFetches(outcomes.toList())
        return OAuthTokenProvider(fetches::fetch) { nowMillis } to fetches
    }

    private fun token(value: String, expiresInSeconds: Long? = 3_600L): () -> OAuthTokenEndpoint.Token =
        { OAuthTokenEndpoint.Token(value, expiresInSeconds) }

    private fun failing(): () -> OAuthTokenEndpoint.Token =
        { throw IllegalStateException("auth endpoint down") }

    @Test
    fun `fetches a token on first use`() = runTest {
        val (provider, fetches) = providerFetching(token("token-1"))

        assertEquals("token-1", provider.getAccessToken())
        assertEquals(1, fetches.count)
    }

    @Test
    fun `serves the cached token while it is fresh`() = runTest {
        val (provider, fetches) = providerFetching(token("token-1"), token("token-2"))

        provider.getAccessToken()
        assertEquals("token-1", provider.getAccessToken())
        assertEquals(1, fetches.count)
    }

    @Test
    fun `refreshes ahead of expiry once inside the refresh margin`() = runTest {
        val (provider, _) = providerFetching(token("token-1", 120), token("token-2", 120))

        provider.getAccessToken()
        nowMillis = 61_000 // 59s left — inside the 60s refresh margin, well before expiry

        assertEquals("token-2", provider.getAccessToken())
    }

    @Test
    fun `defaults the lifetime when the response has no expires_in`() = runTest {
        val (provider, fetches) = providerFetching(token("token-1", null), token("token-2", null))

        provider.getAccessToken()
        nowMillis = 3_539_000 // just under the 3600s default lifetime minus the 60s margin

        assertEquals("token-1", provider.getAccessToken())
        assertEquals(1, fetches.count)
    }

    @Test
    fun `falls back to the cached token when a refresh fails`() = runTest {
        val (provider, _) = providerFetching(token("token-1", 120), failing())

        provider.getAccessToken()
        nowMillis = 70_000 // inside the refresh margin; the cached token lives until 120s

        // A failed refresh must not take down calls the cached token can still authenticate.
        assertEquals("token-1", provider.getAccessToken())
    }

    @Test
    fun `returns null when a fetch fails with no valid cached token`() = runTest {
        val (provider, _) = providerFetching(failing())

        // The RPC proceeds without auth and fails with the server's rejection — that is
        // the session machinery's problem, not the provider's.
        assertNull(provider.getAccessToken())
    }

    @Test
    fun `stops serving the cached token once it expires`() = runTest {
        val (provider, _) = providerFetching(token("token-1", 120), failing())

        provider.getAccessToken()
        nowMillis = 70_000
        provider.getAccessToken()
        nowMillis = 130_000 // past the 120s lifetime; fetches still failing

        assertNull(provider.getAccessToken())
    }

    @Test
    fun `throttles refetching after a failure`() = runTest {
        val (provider, fetches) = providerFetching(token("token-1", 120), failing())

        provider.getAccessToken()
        nowMillis = 70_000
        provider.getAccessToken() // failed refresh
        nowMillis = 72_000 // within the 5s throttle

        assertEquals("token-1", provider.getAccessToken())
        // An auth outage must not turn every RPC into a fetch attempt.
        assertEquals(2, fetches.count)

        nowMillis = 76_000 // throttle over

        provider.getAccessToken()
        assertEquals(3, fetches.count)
    }
}
