// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.ChannelCredentials
import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials
import java.net.URI

/**
 * Represents a parsed Chronicle connection string (chronicle://[user:pass@]host:port[?params]).
 */
data class ChronicleConnectionString(
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
    val disableTls: Boolean = false,
    val apiKey: String? = null,
    val managementPort: Int = DEFAULT_MANAGEMENT_PORT
) {
    companion object {
        private const val DEFAULT_PORT = 35000
        const val DEFAULT_MANAGEMENT_PORT = 8080
        const val DEVELOPMENT_CLIENT = "chronicle-dev-client"
        const val DEVELOPMENT_CLIENT_SECRET = "chronicle-dev-secret"

        /** Development connection string pointing to localhost with TLS disabled. */
        val DEVELOPMENT: ChronicleConnectionString = ChronicleConnectionString(
            host = "localhost",
            port = DEFAULT_PORT,
            username = DEVELOPMENT_CLIENT,
            password = DEVELOPMENT_CLIENT_SECRET,
            disableTls = true
        )

        /**
         * Parses a chronicle:// connection string.
         *
         * @param connectionString The connection string to parse.
         * @return The parsed [ChronicleConnectionString].
         */
        fun parse(connectionString: String): ChronicleConnectionString {
            val normalized = if (connectionString.startsWith("chronicle://")) {
                connectionString.replaceFirst("chronicle://", "http://")
            } else {
                throw IllegalArgumentException("Connection string must start with 'chronicle://'")
            }

            val uri = URI(normalized)
            val host = uri.host ?: "localhost"
            val port = if (uri.port > 0) uri.port else DEFAULT_PORT

            var username: String? = null
            var password: String? = null
            val userInfo = uri.userInfo
            if (!userInfo.isNullOrEmpty()) {
                val colonIndex = userInfo.indexOf(':')
                if (colonIndex >= 0) {
                    username = userInfo.substring(0, colonIndex)
                    password = userInfo.substring(colonIndex + 1)
                } else {
                    username = userInfo
                }
            }

            var disableTls = false
            var apiKey: String? = null
            val query = uri.query
            if (!query.isNullOrEmpty()) {
                query.split("&").forEach { param ->
                    val eqIndex = param.indexOf('=')
                    if (eqIndex > 0) {
                        val key = param.substring(0, eqIndex)
                        val value = param.substring(eqIndex + 1)
                        when (key.lowercase()) {
                            "disabletls" -> disableTls = value.equals("true", ignoreCase = true)
                            "apikey" -> apiKey = value
                        }
                    }
                }
            }

            return ChronicleConnectionString(
                host = host,
                port = port,
                username = username,
                password = password,
                disableTls = disableTls,
                apiKey = apiKey
            )
        }
    }

    /**
     * Creates the appropriate gRPC [ChannelCredentials] based on this connection string's TLS settings.
     */
    fun createCredentials(): ChannelCredentials =
        if (disableTls) InsecureChannelCredentials.create()
        else TlsChannelCredentials.create()

    /** Returns the target address in host:port format. */
    val target: String get() = "$host:$port"
}
