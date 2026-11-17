// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

/**
 * Loads the certificate authorities named by the connection string's `caCertificatePath` option.
 *
 * The file holds one or more CA certificates in PEM or PKCS#7 format. They replace the platform trust
 * store when certificate validation is on (`skipTlsValidation=false`).
 */
internal object CertificateAuthorities {
    /**
     * Reads the file at [path] and returns trust managers that accept only certificates chaining to it.
     *
     * @throws IllegalArgumentException when the file is missing, unreadable, or holds no certificates.
     */
    fun load(path: String): Array<TrustManager> {
        val bytes = try {
            Files.readAllBytes(Paths.get(path))
        } catch (error: IOException) {
            throw IllegalArgumentException("Cannot read the CA certificate file '$path'.", error)
        }

        val certificates = try {
            CertificateFactory.getInstance("X.509").generateCertificates(ByteArrayInputStream(bytes))
        } catch (error: CertificateException) {
            throw IllegalArgumentException("The CA certificate file '$path' is not valid PEM or PKCS#7.", error)
        }

        require(certificates.isNotEmpty()) { "The CA certificate file '$path' does not contain any certificates." }

        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        certificates.forEachIndexed { index, certificate -> store.setCertificateEntry("ca-$index", certificate) }

        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(store) }
            .trustManagers
    }
}
