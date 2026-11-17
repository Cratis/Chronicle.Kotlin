// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.ChannelCredentials
import io.grpc.TlsChannelCredentials
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * The TLS material a connection string names: the client identity from `certificatePath` and the
 * certificate authorities from `caCertificatePath`.
 *
 * It is loaded once when the connection is created, so a missing or unreadable file fails the
 * connection setup instead of every reconnect - and also when validation is skipped or TLS is
 * disabled, so the option cannot lie dormant until validation is switched on.
 */
internal class ChronicleTls private constructor(
    private val keyManagers: Array<KeyManager>?,
    private val certificateAuthorities: Array<TrustManager>?
) {
    /** The trust managers to verify the server with: accept anything, the configured authorities, or the platform default. */
    fun trustManagers(skipTlsValidation: Boolean): Array<TrustManager>? =
        if (skipTlsValidation) arrayOf(InsecureTrustManager()) else certificateAuthorities

    /** Whether this configures anything beyond the platform defaults. */
    val isEmpty: Boolean get() = keyManagers == null && certificateAuthorities == null

    /** The [SSLContext] for the HTTP clients probing load balancer candidates and fetching tokens. */
    fun sslContext(skipTlsValidation: Boolean): SSLContext = SSLContext.getInstance("TLS").apply {
        init(keyManagers, trustManagers(skipTlsValidation), null)
    }

    /** The channel credentials for TLS, with the client identity and trust configured. */
    fun channelCredentials(skipTlsValidation: Boolean): ChannelCredentials {
        val builder = TlsChannelCredentials.newBuilder()
        keyManagers?.let { builder.keyManager(*it) }
        trustManagers(skipTlsValidation)?.let { builder.trustManager(*it) }
        return builder.build()
    }

    companion object {
        /** No TLS material beyond the platform defaults. */
        val NONE = ChronicleTls(null, null)

        /** Loads the TLS material [connectionString] names. */
        fun load(connectionString: ChronicleConnectionString): ChronicleTls {
            val authorities = connectionString.caCertificatePath?.let(CertificateAuthorities::load)
            val identity = connectionString.certificatePath?.let { ClientCertificate.load(it, connectionString.certificatePassword) }
            return if (authorities == null && identity == null) NONE else ChronicleTls(identity, authorities)
        }
    }
}
