// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory

/**
 * Loads the client certificate named by the connection string's `certificatePath` option, for
 * mutual TLS.
 *
 * Two formats are accepted:
 * - a PKCS#12 file (`.p12`/`.pfx`) holding the certificate and its private key, and
 * - a PEM file holding the certificate chain and an unencrypted or encrypted PKCS#8 private key
 *   (`BEGIN PRIVATE KEY` / `BEGIN ENCRYPTED PRIVATE KEY`), in any order.
 *
 * `certificatePassword` unlocks a PKCS#12 file or an encrypted PEM key.
 */
internal object ClientCertificate {
    private val pemBlock = Regex("-----BEGIN ([A-Z0-9 ]+)-----(.*?)-----END \\1-----", RegexOption.DOT_MATCHES_ALL)
    private val keyAlgorithms = listOf("RSA", "EC", "Ed25519", "DSA")

    /**
     * Reads the certificate at [path] and returns the key managers presenting it.
     *
     * @throws IllegalArgumentException when the file is missing, unreadable, or does not hold a usable identity.
     */
    fun load(path: String, password: String?): Array<KeyManager> {
        val bytes = try {
            Files.readAllBytes(Paths.get(path))
        } catch (error: IOException) {
            throw IllegalArgumentException("Cannot read the client certificate '$path'.", error)
        }

        val store = try {
            if (String(bytes, Charsets.ISO_8859_1).contains("-----BEGIN")) loadPem(bytes, password, path) else loadPkcs12(bytes, password, path)
        } catch (error: GeneralSecurityException) {
            throw IllegalArgumentException("The client certificate '$path' could not be loaded: ${error.message}", error)
        } catch (error: IOException) {
            throw IllegalArgumentException("The client certificate '$path' could not be loaded: ${error.message}", error)
        }

        // A PKCS#12 key shares the store's password; the in-memory PEM store has an empty one.
        val keyPassword = if (store.second) (password ?: "").toCharArray() else CharArray(0)
        return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(store.first, keyPassword) }
            .keyManagers
    }

    private fun loadPkcs12(bytes: ByteArray, password: String?, path: String): Pair<KeyStore, Boolean> {
        val store = KeyStore.getInstance("PKCS12")
        try {
            store.load(ByteArrayInputStream(bytes), (password ?: "").toCharArray())
        } catch (error: IOException) {
            throw IllegalArgumentException(
                "The client certificate '$path' is not a valid PKCS#12 file, or the certificatePassword is wrong.",
                error
            )
        }
        return store to true
    }

    private fun loadPem(bytes: ByteArray, password: String?, path: String): Pair<KeyStore, Boolean> {
        val factory = CertificateFactory.getInstance("X.509")
        val certificates = mutableListOf<Certificate>()
        var key: PrivateKey? = null

        for (block in pemBlock.findAll(String(bytes, Charsets.ISO_8859_1))) {
            val type = block.groupValues[1]
            val der = Base64.getMimeDecoder().decode(block.groupValues[2])
            when (type) {
                "CERTIFICATE" -> certificates.add(factory.generateCertificate(ByteArrayInputStream(der)))
                "PRIVATE KEY" -> key = privateKey(PKCS8EncodedKeySpec(der), path)
                "ENCRYPTED PRIVATE KEY" -> {
                    requireNotNull(password) { "The client certificate '$path' has an encrypted private key; set certificatePassword." }
                    key = privateKey(decrypt(der, password), path)
                }
                "RSA PRIVATE KEY", "EC PRIVATE KEY" -> throw IllegalArgumentException(
                    "The client certificate '$path' holds a traditional '$type'; convert it to PKCS#8 with 'openssl pkcs8 -topk8'."
                )
            }
        }

        require(certificates.isNotEmpty()) { "The client certificate '$path' does not contain a certificate." }
        val privateKey = requireNotNull(key) { "The client certificate '$path' does not contain a private key." }

        // The bundle may list the chain in any order; the leaf is the certificate the key belongs to.
        val leaf = requireNotNull(certificates.firstOrNull { matches(privateKey, it) }) {
            "The private key in the client certificate '$path' does not match any of its certificates."
        }
        val chain = listOf(leaf) + certificates.filter { it !== leaf }

        val store = KeyStore.getInstance("PKCS12").apply { load(null, null) }
        store.setKeyEntry("client", privateKey, CharArray(0), chain.toTypedArray())
        return store to false
    }

    private fun decrypt(der: ByteArray, password: String): PKCS8EncodedKeySpec {
        val info = EncryptedPrivateKeyInfo(der)
        // PKCS#8 encrypted with PBES2 names the scheme generically; the parameters spell out the
        // concrete cipher, e.g. PBEWithHmacSHA256AndAES_256.
        val algorithm = if (info.algName.equals("PBES2", ignoreCase = true)) info.algParameters.toString() else info.algName
        val secret = SecretKeyFactory.getInstance(algorithm).generateSecret(PBEKeySpec(password.toCharArray()))
        val cipher = Cipher.getInstance(algorithm).apply { init(Cipher.DECRYPT_MODE, secret, info.algParameters) }
        return info.getKeySpec(cipher)
    }

    private fun privateKey(spec: PKCS8EncodedKeySpec, path: String): PrivateKey {
        for (algorithm in keyAlgorithms) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec)
            } catch (_: GeneralSecurityException) {
                // Not this key type; try the next one.
            }
        }
        throw IllegalArgumentException("The private key in the client certificate '$path' is of an unsupported type.")
    }

    private fun matches(key: PrivateKey, certificate: Certificate): Boolean {
        val algorithm = when (key.algorithm) {
            "RSA" -> "SHA256withRSA"
            "EC" -> "SHA256withECDSA"
            "DSA" -> "SHA256withDSA"
            else -> key.algorithm
        }
        val probe = ByteArray(32) { it.toByte() }
        return try {
            val signature = Signature.getInstance(algorithm).apply { initSign(key); update(probe) }.sign()
            Signature.getInstance(algorithm).apply { initVerify(certificate.publicKey); update(probe) }.verify(signature)
        } catch (_: GeneralSecurityException) {
            false
        }
    }
}
