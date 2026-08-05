// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.ChannelCredentials
import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials

/**
 * Represents a parsed Chronicle connection string.
 *
 * Supported grammar:
 * ```
 * chronicle://<host>[:<port>][,<host>[:<port>]...][?<options>]
 * chronicle://<username>:<password>@<host>[:<port>][,...][?<options>]
 * chronicle+srv://<host>[:<port>][?<options>]
 * ```
 *
 * `java.net.URI` cannot parse a comma-separated multi-host authority, so [parse] is hand-rolled
 * rather than delegated to it.
 */
data class ChronicleConnectionString(
    val addresses: List<ChronicleServerAddress>,
    val username: String? = null,
    val password: String? = null,
    val disableTls: Boolean = false,
    val skipTlsValidation: Boolean = true,
    val apiKey: String? = null,
    val loadBalancer: LoadBalancer = LoadBalancer.LEAST_CONNECTIONS,
    val srvNameServer: String? = null,
    val isSrv: Boolean = false
) {
    init {
        require(addresses.isNotEmpty()) { "A Chronicle connection string must specify at least one server address." }
    }

    companion object {
        private const val DEFAULT_PORT = 35000
        private const val SCHEME = "chronicle://"
        private const val SRV_SCHEME = "chronicle+srv://"
        const val DEVELOPMENT_CLIENT = "chronicle-dev-client"
        const val DEVELOPMENT_CLIENT_SECRET = "chronicle-dev-secret"

        /**
         * Development connection string pointing to localhost over TLS with the standard dev
         * credentials. Certificate validation is skipped by default (see [skipTlsValidation]),
         * so this connects to the Kernel's self-signed development certificate without further
         * configuration.
         */
        val DEVELOPMENT: ChronicleConnectionString = ChronicleConnectionString(
            addresses = listOf(ChronicleServerAddress("localhost", DEFAULT_PORT)),
            username = DEVELOPMENT_CLIENT,
            password = DEVELOPMENT_CLIENT_SECRET
        )

        /**
         * Parses a `chronicle://` or `chronicle+srv://` connection string.
         *
         * @param connectionString The connection string to parse.
         * @return The parsed [ChronicleConnectionString].
         */
        fun parse(connectionString: String): ChronicleConnectionString {
            val isSrv = connectionString.startsWith(SRV_SCHEME)
            val schemeLength = when {
                isSrv -> SRV_SCHEME.length
                connectionString.startsWith(SCHEME) -> SCHEME.length
                else -> throw IllegalArgumentException(
                    "Connection string must start with '$SCHEME' or '$SRV_SCHEME'"
                )
            }
            var remainder = connectionString.substring(schemeLength)

            val queryIndex = remainder.indexOf('?')
            val query = if (queryIndex >= 0) remainder.substring(queryIndex + 1) else null
            if (queryIndex >= 0) remainder = remainder.substring(0, queryIndex)

            var username: String? = null
            var password: String? = null
            val atIndex = remainder.lastIndexOf('@')
            if (atIndex >= 0) {
                val userInfo = remainder.substring(0, atIndex)
                remainder = remainder.substring(atIndex + 1)
                val colonIndex = userInfo.indexOf(':')
                if (colonIndex >= 0) {
                    username = userInfo.substring(0, colonIndex).ifEmpty { null }
                    password = userInfo.substring(colonIndex + 1)
                } else {
                    username = userInfo.ifEmpty { null }
                }
            }

            val addresses = remainder.split(",").filter { it.isNotEmpty() }.map(::parseHostAndPort)
            require(addresses.isNotEmpty()) { "Connection string must specify at least one host." }
            require(!isSrv || addresses.size == 1) {
                "'$SRV_SCHEME' connection strings support only a single host."
            }

            var disableTls = false
            var skipTlsValidation = true
            var apiKey: String? = null
            var loadBalancer = LoadBalancer.LEAST_CONNECTIONS
            var srvNameServer: String? = null
            if (!query.isNullOrEmpty()) {
                query.split("&").forEach { param ->
                    val eqIndex = param.indexOf('=')
                    if (eqIndex > 0) {
                        val key = param.substring(0, eqIndex)
                        val value = param.substring(eqIndex + 1)
                        when (key.lowercase()) {
                            "disabletls" -> disableTls = value.equals("true", ignoreCase = true)
                            "skiptlsvalidation" -> skipTlsValidation = value.equals("true", ignoreCase = true)
                            "apikey" -> apiKey = value
                            "loadbalancer" -> loadBalancer = LoadBalancer.parse(value)
                            "srvnameserver" -> srvNameServer = value
                        }
                    }
                }
            }

            return ChronicleConnectionString(
                addresses = addresses,
                username = username,
                password = password,
                disableTls = disableTls,
                skipTlsValidation = skipTlsValidation,
                apiKey = apiKey,
                loadBalancer = loadBalancer,
                srvNameServer = srvNameServer,
                isSrv = isSrv
            )
        }

        /** Parses a single `host[:port]` entry, honoring `[host]:port` bracket notation for IPv6 literals. */
        private fun parseHostAndPort(entry: String): ChronicleServerAddress {
            if (entry.startsWith("[")) {
                val closeIndex = entry.indexOf(']')
                require(closeIndex > 0) { "Invalid IPv6 host literal: '$entry'" }
                val host = entry.substring(1, closeIndex)
                val portPart = entry.substring(closeIndex + 1)
                val port = if (portPart.startsWith(":")) portPart.substring(1).toInt() else DEFAULT_PORT
                return ChronicleServerAddress(host, port)
            }

            val colonIndex = entry.indexOf(':')
            return if (colonIndex >= 0) {
                ChronicleServerAddress(entry.substring(0, colonIndex), entry.substring(colonIndex + 1).toInt())
            } else {
                ChronicleServerAddress(entry, DEFAULT_PORT)
            }
        }
    }

    /**
     * The first configured server address's host.
     *
     * A convenience accessor for callers that only care about a single address; for
     * `chronicle+srv://` connection strings this is the host DNS SRV records are resolved from.
     * See [addresses] for the full list.
     */
    val host: String get() = addresses.first().host

    /**
     * The first configured server address's port.
     *
     * Ignored for `chronicle+srv://` connection strings — DNS SRV records supply the real port
     * for every resolved target.
     */
    val port: Int get() = addresses.first().port

    /**
     * Creates the appropriate gRPC [ChannelCredentials] based on this connection string's TLS settings.
     *
     * By default (`disableTls=false`, `skipTlsValidation=true`) the client connects over TLS but
     * accepts any certificate via [InsecureTrustManager], including self-signed ones. Set
     * `skipTlsValidation=false` to require full certificate chain validation against the platform
     * default trust manager instead — only do so against a server whose certificate is verifiable.
     */
    fun createCredentials(): ChannelCredentials = when {
        disableTls -> InsecureChannelCredentials.create()
        skipTlsValidation -> TlsChannelCredentials.newBuilder().trustManager(InsecureTrustManager()).build()
        else -> TlsChannelCredentials.create()
    }

    /** Renders the first configured server address as `host:port` (IPv6 hosts in bracket notation). */
    val target: String get() = addresses.first().toString()

    /**
     * Renders this connection string back to its `chronicle://`/`chronicle+srv://` textual form.
     *
     * The result is not guaranteed to be byte-identical to whatever string was originally
     * [parse]d (e.g. a host without an explicit port is rendered with the resolved default
     * port), but re-parsing it always yields an equal [ChronicleConnectionString].
     */
    override fun toString(): String = buildString {
        append(if (isSrv) SRV_SCHEME else SCHEME)

        if (username != null || password != null) {
            append(username ?: "")
            if (password != null) append(':').append(password)
            append('@')
        }

        append(addresses.joinToString(","))

        val params = buildList {
            if (disableTls) add("disableTls=true")
            if (!skipTlsValidation) add("skipTlsValidation=false")
            apiKey?.let { add("apiKey=$it") }
            if (loadBalancer != LoadBalancer.LEAST_CONNECTIONS) add("loadBalancer=${loadBalancer.toConnectionStringValue()}")
            srvNameServer?.let { add("srvNameServer=$it") }
        }
        if (params.isNotEmpty()) append('?').append(params.joinToString("&"))
    }
}
