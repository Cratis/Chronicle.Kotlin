// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Builds an [HttpClient] honoring the `disableTls`/`skipTlsValidation` connection string options —
 * shared by [OAuthTokenEndpoint] and [LeastConnectionsLoadBalancerStrategy].
 *
 * When TLS is enabled, [skipTlsValidation] defaults to `true`, accepting any certificate via
 * [InsecureTrustManager]. Set it to `false` to require full certificate chain validation against
 * the platform default trust manager instead.
 */
internal fun createChronicleHttpClient(disableTls: Boolean, skipTlsValidation: Boolean): HttpClient = when {
    disableTls -> HttpClient.newHttpClient()
    skipTlsValidation -> HttpClient.newBuilder().sslContext(InsecureTrustManager.sslContext()).build()
    else -> HttpClient.newHttpClient()
}

/**
 * The OAuth 2.0 token endpoint tokens are fetched from using the client credentials flow.
 *
 * @param tokenEndpoint The full URL of the /connect/token endpoint.
 * @param clientId The OAuth client identifier.
 * @param clientSecret The OAuth client secret.
 * @param disableTls Whether TLS is disabled for the token request.
 * @param skipTlsValidation Whether to accept any TLS certificate for the token request instead of
 *   validating it against the platform default trust manager. Defaults to `true`.
 */
internal class OAuthTokenEndpoint(
    private val tokenEndpoint: String,
    private val clientId: String,
    private val clientSecret: String,
    disableTls: Boolean = false,
    skipTlsValidation: Boolean = true
) {
    /**
     * An access token as returned by the endpoint.
     *
     * @property accessToken The Bearer token value.
     * @property expiresInSeconds The token's lifetime — `expires_in` is recommended but not
     *   required by OAuth2, so it can be absent.
     */
    internal data class Token(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_in") val expiresInSeconds: Long? = null
    )

    private val httpClient = createChronicleHttpClient(disableTls, skipTlsValidation)
    private val gson = Gson()

    /** Fetches a fresh token, throwing when the endpoint cannot produce one. */
    fun fetch(): Token {
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

        return gson.fromJson(response.body(), Token::class.java)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
