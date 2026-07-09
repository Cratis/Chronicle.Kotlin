// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertPathBuilderException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.PKIXReason
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * Trust manager that tolerates the Chronicle Kernel's auto-generated, self-signed development
 * certificate while otherwise validating exactly like the platform default trust manager.
 *
 * Mirrors the reference .NET client's `RemoteCertificateValidationCallback`: it bypasses a chain
 * failure only when the *sole* problem is a self-signed/untrusted root (or a partial chain missing
 * an intermediate), and bypasses a hostname mismatch only when that is the *sole* problem. If both
 * a chain failure and a hostname mismatch occur together, or if the chain fails for any other
 * reason (expired, revoked, tampered), validation is rejected exactly as the default trust manager
 * would reject it.
 */
class SelfSignedTrustManager(
    private val expectedHost: String,
    private val delegate: X509TrustManager = defaultTrustManager()
) : X509ExtendedTrustManager() {

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        delegate.checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) =
        delegate.checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) =
        delegate.checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) =
        verify(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) =
        verify(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) =
        verify(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers

    private fun verify(chain: Array<out X509Certificate>, authType: String) {
        val chainFailure = try {
            delegate.checkServerTrusted(chain, authType)
            null
        } catch (failure: CertificateException) {
            failure
        }
        val hostnameMismatch = !hostMatches(expectedHost, chain[0])

        when {
            chainFailure == null && !hostnameMismatch -> return
            chainFailure != null && hostnameMismatch -> throw chainFailure
            chainFailure != null -> if (!isTolerableChainFailure(chain, chainFailure)) throw chainFailure
            else -> Unit // Hostname mismatch alone is tolerated, mirroring the .NET reference.
        }
    }

    /** Tolerable only when the chain fails purely because no trust anchor could be found, and every certificate is otherwise currently valid. */
    private fun isTolerableChainFailure(chain: Array<out X509Certificate>, failure: CertificateException): Boolean {
        if (!isTrustAnchorFailure(failure)) return false
        return chain.all { isCurrentlyValid(it) }
    }

    private fun isTrustAnchorFailure(failure: Throwable?): Boolean {
        var cause = failure
        while (cause != null) {
            if (cause is CertPathBuilderException) return true
            if (cause is CertPathValidatorException && cause.reason == PKIXReason.NO_TRUST_ANCHOR) return true
            cause = cause.cause
        }
        return false
    }

    private fun isCurrentlyValid(certificate: X509Certificate): Boolean = try {
        certificate.checkValidity()
        true
    } catch (_: CertificateException) {
        false
    }

    private fun hostMatches(host: String, certificate: X509Certificate): Boolean {
        val alternativeNames = try {
            certificate.subjectAlternativeNames
        } catch (_: CertificateException) {
            null
        } ?: return false
        val hostIsIpLiteral = IP_LITERAL.matches(host)
        return alternativeNames.any { entry ->
            val type = entry[0] as Int
            val value = entry[1] as String
            when {
                hostIsIpLiteral && type == SAN_IP_ADDRESS -> value.equals(host, ignoreCase = true)
                !hostIsIpLiteral && type == SAN_DNS_NAME -> matchesDnsName(host, value)
                else -> false
            }
        }
    }

    private fun matchesDnsName(host: String, pattern: String): Boolean = when {
        pattern.equals(host, ignoreCase = true) -> true
        pattern.startsWith("*.") -> host.length > pattern.length - 1 &&
            host.endsWith(pattern.substring(1), ignoreCase = true)
        else -> false
    }

    companion object {
        // Type tags from the RFC 5280 GeneralName ASN.1 encoding used by
        // X509Certificate.getSubjectAlternativeNames().
        private const val SAN_DNS_NAME = 2
        private const val SAN_IP_ADDRESS = 7
        private val IP_LITERAL = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

        private fun defaultTrustManager(): X509TrustManager {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
        }
    }
}
