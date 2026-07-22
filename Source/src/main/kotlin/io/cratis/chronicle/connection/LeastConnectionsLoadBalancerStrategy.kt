// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random

/**
 * Selects the server address with the fewest active client connections.
 *
 * Probes every candidate's `/connections/count` endpoint concurrently. A small random delay before
 * every probe attempt protects against a fleet that starts every instance at once (a rollout) or
 * reconnects at once (every instance was waiting for the same server and now retries in lockstep):
 * without it, sibling instances can still probe within microseconds of each other, before either has
 * reserved anything, and tie. The delay is not limited to the first attempt for exactly this reason -
 * a synchronized retry after a shared failure needs the same protection as a cold start. Any
 * remaining tie after probing is broken randomly rather than always preferring the first candidate.
 * The winner is then asked to reserve a slot via `/connections/reserve`, closing the race where a
 * second client probes while the first is still mid-handshake and hasn't registered as connected yet.
 *
 * Both HTTP calls are best-effort: a server that fails to answer, times out, or doesn't implement
 * the endpoints is treated as maximally loaded - never preferred over one that actually responded -
 * rather than failing selection outright.
 *
 * @param disableTls Whether to probe over plain HTTP instead of HTTPS, mirroring the connection
 *   string's `disableTls` option.
 * @param skipTlsValidation Whether the default [httpClient] should skip TLS certificate
 *   validation, mirroring the connection string's `skipTlsValidation` option.
 * @param maxSelectionJitterMs Upper bound in milliseconds for the random delay before every probe.
 *   Defaults to 250ms; pass 0 to disable it.
 * @param httpClient The client used to probe candidates. Overridable for testing so specs don't
 *   need a real server.
 */
class LeastConnectionsLoadBalancerStrategy(
    private val disableTls: Boolean = false,
    private val skipTlsValidation: Boolean = true,
    private val maxSelectionJitterMs: Int = DEFAULT_MAX_SELECTION_JITTER_MS,
    private val httpClient: HttpClient = createChronicleHttpClient(disableTls, skipTlsValidation)
) : LoadBalancerStrategy {

    override suspend fun select(addresses: List<ChronicleServerAddress>): ChronicleServerAddress {
        require(addresses.isNotEmpty()) { "Cannot select an address from an empty list." }
        if (addresses.size == 1) return addresses.first()

        if (maxSelectionJitterMs > 0) delay(Random.nextLong(maxSelectionJitterMs.toLong()))

        val counts = withContext(Dispatchers.IO) {
            addresses.map { address -> async { address to connectionCount(address) } }.awaitAll()
        }

        val lowest = counts.minOf { it.second }
        val selected = counts.filter { it.second == lowest }.map { it.first }.random()

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
        if (response.statusCode() == 200) response.body().trim().toIntOrNull() ?: Int.MAX_VALUE else Int.MAX_VALUE
    } catch (_: Exception) {
        // Unreachable or too slow to answer - treat as maximally loaded so it is never picked
        // over a server that actually responded, rather than failing the whole selection.
        Int.MAX_VALUE
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
        private const val DEFAULT_MAX_SELECTION_JITTER_MS = 250
        private val TIMEOUT: Duration = Duration.ofSeconds(2)
    }
}
