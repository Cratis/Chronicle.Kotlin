// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random

private data class ConnectionCount(val count: Int)

/**
 * Selects the server address with the fewest active client connections.
 *
 * Probes every candidate's `/connections/count` endpoint concurrently. A small random jitter is
 * added to each observed count before comparing, so that many clients starting at once don't all
 * converge on exactly the same "least loaded" server; any remaining tie is broken randomly. The
 * winner is then asked to reserve a slot via `/connections/reserve`, so a burst of concurrent
 * client startups doesn't all pick the same server before any of their connections actually land.
 *
 * Both HTTP calls are best-effort: a server that fails to answer, times out, or doesn't implement
 * the endpoints is treated as having zero connections rather than failing selection outright.
 *
 * @param disableTls Whether to probe over plain HTTP instead of HTTPS, mirroring the connection
 *   string's `disableTls` option.
 * @param skipTlsValidation Whether the default [httpClient] should skip TLS certificate
 *   validation, mirroring the connection string's `skipTlsValidation` option.
 * @param httpClient The client used to probe candidates. Overridable for testing so specs don't
 *   need a real server.
 */
class LeastConnectionsLoadBalancerStrategy(
    private val disableTls: Boolean = false,
    private val skipTlsValidation: Boolean = false,
    private val httpClient: HttpClient = createChronicleHttpClient(disableTls, skipTlsValidation)
) : LoadBalancerStrategy {

    override suspend fun select(addresses: List<ChronicleServerAddress>): ChronicleServerAddress {
        require(addresses.isNotEmpty()) { "Cannot select an address from an empty list." }
        if (addresses.size == 1) return addresses.first()

        val jitteredCounts = withContext(Dispatchers.IO) {
            addresses.map { address -> async { address to (connectionCount(address) + jitter()) } }.awaitAll()
        }

        val lowest = jitteredCounts.minOf { it.second }
        val selected = jitteredCounts.filter { it.second == lowest }.map { it.first }.random()

        withContext(Dispatchers.IO) { reserve(selected) }

        return selected
    }

    private fun connectionCount(address: ChronicleServerAddress): Int = try {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${scheme()}://${address.host}:${address.port}$COUNT_PATH"))
            .timeout(TIMEOUT)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) gson.fromJson(response.body(), ConnectionCount::class.java).count else 0
    } catch (_: Exception) {
        0
    }

    private fun reserve(address: ChronicleServerAddress) {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${scheme()}://${address.host}:${address.port}$RESERVE_PATH"))
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        } catch (_: Exception) {
            // Best-effort — a failed reservation just means the next probe sees a stale count.
        }
    }

    private fun scheme() = if (disableTls) "http" else "https"

    companion object {
        private const val COUNT_PATH = "/connections/count"
        private const val RESERVE_PATH = "/connections/reserve"
        private const val JITTER_BOUND = 3
        private val TIMEOUT: Duration = Duration.ofSeconds(2)
        private val gson = Gson()

        private fun jitter(): Int = Random.nextInt(JITTER_BOUND)
    }
}
