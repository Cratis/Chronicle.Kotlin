// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.ChannelCredentials
import io.grpc.InsecureChannelCredentials
import java.net.URLDecoder
import java.net.URLEncoder

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
data class ChronicleConnectionString @JvmOverloads constructor(
    val addresses: List<ChronicleServerAddress>,
    val username: String? = null,
    val password: String? = null,
    val disableTls: Boolean = false,
    val skipTlsValidation: Boolean = true,
    val apiKey: String? = null,
    val loadBalancer: LoadBalancer = LoadBalancer.LEAST_CONNECTIONS,
    val srvNameServer: String? = null,
    val isSrv: Boolean = false,
    /** Path to a PKCS#12 or PEM file with the client certificate presented to the server (mutual TLS). */
    val certificatePath: String? = null,
    /** Password for [certificatePath]: unlocks a PKCS#12 file or an encrypted PEM private key. */
    val certificatePassword: String? = null,
    /**
     * Path to a PEM or PKCS#7 file with the certificate authorities the server certificate must chain
     * to, in place of the platform trust store. Used only when [skipTlsValidation] is `false`; the file
     * is nevertheless read when the connection is created, so a missing or unreadable file fails the
     * connection setup.
     */
    val caCertificatePath: String? = null,
    /**
     * Whether `auth=none` was given: no authentication at all. It wins over any credentials the
     * connection string also carries, so credentials left behind by shared configuration cannot
     * quietly switch authentication back on.
     */
    val noAuthentication: Boolean = false,
    /**
     * Whether to skip the server compatibility check on connect (`skipCompatibilityCheck=true`). An
     * escape hatch for a known-safe client and kernel skew; it hides every other incompatibility too.
     */
    val skipCompatibilityCheck: Boolean = false,
    /**
     * Whether to skip the long-lived keep-alive session and its watchdog (`skipKeepAlive=true`). Suits
     * short-lived processes such as command line tools and jobs. A client that skips keep-alive cannot
     * host reactors or reducers, as the kernel has no live connection to deliver events on.
     */
    val skipKeepAlive: Boolean = false,
    /** Options this client does not know, kept so they survive a round trip through [toString]. */
    val additionalOptions: Map<String, String> = emptyMap()
) {
    init {
        require(addresses.isNotEmpty()) { "A Chronicle connection string must specify at least one server address." }
    }

    companion object {
        private const val DEFAULT_PORT = 35000
        private const val SCHEME = "chronicle://"
        private const val SRV_SCHEME = "chronicle+srv://"
        private const val NO_AUTHENTICATION = "none"

        /** Options whose value may legitimately hold an at sign, such as a path or a name server. */
        private val optionsThatMayHoldAtSign =
            setOf("apikey", "certificatepath", "certificatepassword", "cacertificatepath", "loadbalancer", "srvnameserver")
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

            // An at sign in the query means the user information held a separator that was not
            // percent-encoded and spilled into it - unless the option is one that may hold one.
            if (query != null && query.split("&").any(::hasMisplacedAtSign)) {
                throw IllegalArgumentException(
                    "Connection string user information must be percent-encoded: it contains a separator."
                )
            }

            var username: String? = null
            var password: String? = null
            val atIndex = remainder.lastIndexOf('@')
            if (atIndex >= 0) {
                val userInfo = remainder.substring(0, atIndex)
                remainder = remainder.substring(atIndex + 1)
                val colonIndex = userInfo.indexOf(':')
                if (colonIndex >= 0) {
                    username = decode(userInfo.substring(0, colonIndex)).ifEmpty { null }
                    password = decode(userInfo.substring(colonIndex + 1))
                } else {
                    username = decode(userInfo).ifEmpty { null }
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
            var certificatePath: String? = null
            var certificatePassword: String? = null
            var caCertificatePath: String? = null
            var noAuthentication = false
            var skipCompatibilityCheck = false
            var skipKeepAlive = false
            val additionalOptions = linkedMapOf<String, String>()

            if (!query.isNullOrEmpty()) {
                query.split("&").forEach { param ->
                    val eqIndex = param.indexOf('=')
                    if (eqIndex > 0) {
                        val key = decode(param.substring(0, eqIndex))
                        val value = decode(param.substring(eqIndex + 1))
                        when (key.lowercase()) {
                            "disabletls" -> disableTls = parseBoolean(key, value)
                            "skiptlsvalidation" -> skipTlsValidation = parseBoolean(key, value)
                            "skipcompatibilitycheck" -> skipCompatibilityCheck = parseBoolean(key, value)
                            "skipkeepalive" -> skipKeepAlive = parseBoolean(key, value)
                            "auth" -> noAuthentication = value.equals(NO_AUTHENTICATION, ignoreCase = true)
                            "apikey" -> apiKey = value
                            "loadbalancer" -> loadBalancer = LoadBalancer.parse(value)
                            "srvnameserver" -> srvNameServer = value
                            "certificatepath" -> certificatePath = value.ifEmpty { null }
                            "certificatepassword" -> certificatePassword = value.ifEmpty { null }
                            "cacertificatepath" -> caCertificatePath = value.ifEmpty { null }
                            else -> additionalOptions[key] = value
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
                isSrv = isSrv,
                certificatePath = certificatePath,
                certificatePassword = certificatePassword,
                caCertificatePath = caCertificatePath,
                noAuthentication = noAuthentication,
                skipCompatibilityCheck = skipCompatibilityCheck,
                skipKeepAlive = skipKeepAlive,
                additionalOptions = additionalOptions
            )
        }

        private fun hasMisplacedAtSign(pair: String): Boolean {
            if (!pair.contains('@')) return false
            val eqIndex = pair.indexOf('=')
            if (eqIndex == -1 || pair.indexOf('@') < eqIndex) return true
            return decode(pair.substring(0, eqIndex)).lowercase() !in optionsThatMayHoldAtSign
        }

        private fun parseBoolean(key: String, value: String): Boolean = when (value.lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("The value of the '$key' option is invalid.")
        }

        /** Percent-decodes like `Uri.UnescapeDataString`: a literal `+` stays a plus. */
        private fun decode(value: String): String = URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8)

        /** Percent-encodes like `Uri.EscapeDataString`: only the unreserved characters stay as they are. */
        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

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
    fun createCredentials(): ChannelCredentials = createCredentials(ChronicleTls.load(this))

    internal fun createCredentials(tls: ChronicleTls): ChannelCredentials = when {
        disableTls -> InsecureChannelCredentials.create()
        else -> tls.channelCredentials(skipTlsValidation)
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
            append(encode(username ?: ""))
            if (password != null) append(':').append(encode(password))
            append('@')
        }

        append(addresses.joinToString(","))

        val params = buildList {
            if (disableTls) add("disableTls=true")
            if (!skipTlsValidation) add("skipTlsValidation=false")
            if (noAuthentication) add("auth=$NO_AUTHENTICATION")
            apiKey?.let { add("apiKey=${encode(it)}") }
            if (skipCompatibilityCheck) add("skipCompatibilityCheck=true")
            if (skipKeepAlive) add("skipKeepAlive=true")
            if (loadBalancer != LoadBalancer.LEAST_CONNECTIONS) add("loadBalancer=${loadBalancer.toConnectionStringValue()}")
            srvNameServer?.let { add("srvNameServer=${encode(it)}") }
            certificatePath?.let { add("certificatePath=${encode(it)}") }
            certificatePassword?.let { add("certificatePassword=${encode(it)}") }
            caCertificatePath?.let { add("caCertificatePath=${encode(it)}") }
            additionalOptions.forEach { (key, value) -> add("${encode(key)}=${encode(value)}") }
        }
        if (params.isNotEmpty()) append('?').append(params.joinToString("&"))
    }
}
