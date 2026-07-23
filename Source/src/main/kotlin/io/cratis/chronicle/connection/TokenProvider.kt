// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Refresh once the token has less than this long left before it expires. */
private const val TOKEN_REFRESH_MARGIN_MS = 60_000L

/** Assumed lifetime when the token response carries no `expires_in`. */
private const val DEFAULT_TOKEN_EXPIRY_SECONDS = 3_600L

/** Minimum pause between failed fetch attempts. */
private const val FAILED_FETCH_RETRY_DELAY_MS = 5_000L

/** Provides Bearer tokens for gRPC calls. */
interface ITokenProvider {
    /**
     * Returns the current access token, or null when authentication is not needed or no
     * token can be obtained — the RPC then proceeds without auth and fails with the
     * server's rejection, which the session machinery handles.
     */
    suspend fun getAccessToken(): String?
}

/** No-op provider used when there are no credentials. */
object NoOpTokenProvider : ITokenProvider {
    override suspend fun getAccessToken(): String? = null
}

/**
 * Fetches and caches access tokens using the OAuth 2.0 client credentials flow.
 *
 * The token is fetched lazily on the first request and refreshed on demand once it enters
 * the refresh margin. RPCs flow continuously (the session answers a keep-alive every
 * second), so on-demand refresh is proactive in practice: the token is renewed within a
 * second of entering the margin, long before it expires. Fetch failures fall back to the
 * cached token while it is still valid, and retries are throttled so an auth outage does
 * not turn every RPC into a fetch attempt.
 */
class OAuthTokenProvider internal constructor(
    private val fetchToken: () -> OAuthTokenEndpoint.Token,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : ITokenProvider {

    /**
     * Creates a provider fetching from the given endpoint.
     *
     * @param tokenEndpoint The full URL of the /connect/token endpoint.
     * @param clientId The OAuth client identifier.
     * @param clientSecret The OAuth client secret.
     * @param disableTls Whether TLS is disabled for the token request.
     * @param skipTlsValidation Whether to accept any TLS certificate for the token request
     *   instead of validating it against the platform default trust manager. Defaults to `true`.
     */
    constructor(
        tokenEndpoint: String,
        clientId: String,
        clientSecret: String,
        disableTls: Boolean = false,
        skipTlsValidation: Boolean = true
    ) : this(OAuthTokenEndpoint(tokenEndpoint, clientId, clientSecret, disableTls, skipTlsValidation)::fetch)

    private val mutex = Mutex()

    private var cachedToken: String? = null
    private var refreshAtMillis = 0L
    private var expiresAtMillis = 0L
    private var lastFailedFetchMillis: Long? = null

    override suspend fun getAccessToken(): String? = mutex.withLock {
        val now = currentTimeMillis()
        when {
            fresh(now) -> cachedToken
            throttled(now) -> stillValidCachedToken(now)
            else -> fetchFresh(now)
        }
    }

    private fun fresh(now: Long) = cachedToken != null && now < refreshAtMillis

    private fun throttled(now: Long) =
        lastFailedFetchMillis?.let { now - it < FAILED_FETCH_RETRY_DELAY_MS } ?: false

    private fun stillValidCachedToken(now: Long) = cachedToken?.takeIf { now < expiresAtMillis }

    private fun fetchFresh(now: Long): String? = try {
        val token = fetchToken()
        val lifetimeMillis = (token.expiresInSeconds ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000
        cachedToken = token.accessToken
        expiresAtMillis = now + lifetimeMillis
        refreshAtMillis = expiresAtMillis - TOKEN_REFRESH_MARGIN_MS
        lastFailedFetchMillis = null
        cachedToken
    } catch (e: Exception) {
        // A failed refresh must not take down calls the cached token could still
        // authenticate — serve it while it lives, and let the RPC surface the server's
        // rejection once it is truly gone.
        System.err.println("[Chronicle] Failed to fetch OAuth2 token: ${e.message}")
        lastFailedFetchMillis = now
        stillValidCachedToken(now)
    }
}
