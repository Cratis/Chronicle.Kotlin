// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * Trust manager that accepts any certificate chain and any hostname — no validation at all.
 *
 * Used only when a connection string opts in explicitly via `skipTlsValidation=true`, e.g. to
 * connect through a certificate the client has no way to validate (an internal CA, a hostname
 * that doesn't match the certificate's subject). This is intentionally broader than
 * [SelfSignedTrustManager], which still enforces every check except a self-signed root or a
 * hostname mismatch, never both together. [InsecureTrustManager] enforces nothing and must only
 * ever be opted into deliberately — it is never the default.
 */
class InsecureTrustManager : X509ExtendedTrustManager() {
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) = Unit
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        /** Builds an [SSLContext] that trusts any certificate via [InsecureTrustManager]. */
        fun sslContext(): SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(InsecureTrustManager()), null)
        }
    }
}
