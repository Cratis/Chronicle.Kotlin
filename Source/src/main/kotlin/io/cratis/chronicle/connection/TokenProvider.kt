// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant

private const val TOKEN_EXPIRY_BUFFER_SECONDS = 60L
private const val DEFAULT_TOKEN_EXPIRY_SECONDS = 3600L

private data class OAuthTokenResponse(
    val access_token: String,
    val expires_in: Long? = null
)

/** Provides Bearer tokens for gRPC calls. */
interface ITokenProvider {
    /** Returns the current access token, or null when authentication is not needed. */
    suspend fun getAccessToken(): String?
}

/** No-op provider used when there are no credentials. */
object NoOpTokenProvider : ITokenProvider {
    override suspend fun getAccessToken(): String? = null
}

/**
 * Fetches and caches access tokens using the OAuth 2.0 client credentials flow.
 *
 * @param tokenEndpoint The full URL of the /connect/token endpoint.
 * @param clientId The OAuth client identifier.
 * @param clientSecret The OAuth client secret.
 */
class OAuthTokenProvider(
    private val tokenEndpoint: String,
    private val clientId: String,
    private val clientSecret: String
) : ITokenProvider {

    private val httpClient = HttpClient.newHttpClient()
    private val gson = Gson()
    private val mutex = Mutex()

    private var cachedToken: String? = null
    private var tokenExpiry: Instant = Instant.EPOCH

    override suspend fun getAccessToken(): String? {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken
        }

        return mutex.withLock {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
                return@withLock cachedToken
            }
            fetchToken()
        }
    }

    private fun fetchToken(): String? {
        val body = buildString {
            append(encode("grant_type")).append('=').append(encode("client_credentials"))
            append('&')
            append(encode("client_id")).append('=').append(encode(clientId))
            append('&')
            append(encode("client_secret")).append('=').append(encode(clientSecret))
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(tokenEndpoint))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException(
                "Token request failed with status ${response.statusCode()}: ${response.body()}"
            )
        }

        val tokenResponse = gson.fromJson(response.body(), OAuthTokenResponse::class.java)
        val expiresIn = tokenResponse.expires_in ?: DEFAULT_TOKEN_EXPIRY_SECONDS
        cachedToken = tokenResponse.access_token
        tokenExpiry = Instant.now().plusSeconds(expiresIn - TOKEN_EXPIRY_BUFFER_SECONDS)
        return cachedToken
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
