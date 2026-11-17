// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509TrustManager

class ChronicleTlsTests {
    companion object {
        private var openSslAvailable = false

        @JvmStatic
        @BeforeAll
        fun detectOpenSsl() {
            openSslAvailable = try {
                ProcessBuilder("openssl", "version").redirectErrorStream(true).start().waitFor(10, TimeUnit.SECONDS)
            } catch (_: Exception) {
                false
            }
        }
    }

    @TempDir
    lateinit var directory: Path

    private fun openssl(vararg arguments: String) {
        val process = ProcessBuilder(listOf("openssl") + arguments).directory(directory.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0, "openssl ${arguments.toList()} failed: $output")
    }

    /** Generates a self-signed RSA certificate and its PKCS#8 key, returning (certificate, key). */
    private fun generateIdentity(name: String): Pair<File, File> {
        assumeTrue(openSslAvailable, "openssl is needed to generate test certificates")
        openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", "$name.key", "-out", "$name.pem", "-subj", "/CN=$name", "-days", "2")
        return directory.resolve("$name.pem").toFile() to directory.resolve("$name.key").toFile()
    }

    private fun connectionString(options: String) = ChronicleConnectionString.parse("chronicle://localhost:35000?$options")

    private fun encoded(file: File) = java.net.URLEncoder.encode(file.absolutePath, Charsets.UTF_8)

    @Test
    fun `without certificate options there is no TLS material`() {
        assertSame(ChronicleTls.NONE, ChronicleTls.load(connectionString("skipTlsValidation=false")))
    }

    @Test
    fun `caCertificatePath trusts only the certificate authorities in the file`() {
        val (certificate, _) = generateIdentity("authority")
        val (other, _) = generateIdentity("stranger")

        val tls = ChronicleTls.load(connectionString("caCertificatePath=${encoded(certificate)}"))
        val trust = tls.trustManagers(skipTlsValidation = false)!!.filterIsInstance<X509TrustManager>().single()
        val trusted = java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(certificate.inputStream())
        val untrusted = java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(other.inputStream())

        assertTrue(trust.acceptedIssuers.contains(trusted))
        assertFalse(trust.acceptedIssuers.contains(untrusted))
    }

    @Test
    fun `skipping validation ignores the certificate authorities`() {
        val (certificate, _) = generateIdentity("authority")
        val tls = ChronicleTls.load(connectionString("caCertificatePath=${encoded(certificate)}"))

        assertTrue(tls.trustManagers(skipTlsValidation = true)!!.single() is InsecureTrustManager)
    }

    @Test
    fun `a missing caCertificatePath file fails even while validation is skipped`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("caCertificatePath=%2Fno%2Fsuch.pem"))
        }
        assertTrue(error.message!!.contains("Cannot read"))
    }

    @Test
    fun `a caCertificatePath file without certificates fails`() {
        val file = directory.resolve("empty.pem").toFile().apply { writeText("not a certificate") }

        assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("caCertificatePath=${encoded(file)}"))
        }
    }

    @Test
    fun `loads a PKCS12 client certificate`() {
        val (certificate, key) = generateIdentity("client")
        openssl("pkcs12", "-export", "-in", certificate.name, "-inkey", key.name, "-out", "client.p12", "-passout", "pass:changeit")
        val file = directory.resolve("client.p12").toFile()

        val tls = ChronicleTls.load(connectionString("certificatePath=${encoded(file)}&certificatePassword=changeit"))

        assertFalse(tls.isEmpty)
    }

    @Test
    fun `a wrong PKCS12 password fails`() {
        val (certificate, key) = generateIdentity("client")
        openssl("pkcs12", "-export", "-in", certificate.name, "-inkey", key.name, "-out", "client.p12", "-passout", "pass:changeit")
        val file = directory.resolve("client.p12").toFile()

        assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("certificatePath=${encoded(file)}&certificatePassword=wrong"))
        }
    }

    @Test
    fun `loads a PEM client certificate with the key listed first`() {
        val (certificate, key) = generateIdentity("client")
        val bundle = directory.resolve("bundle.pem").toFile().apply { writeText(key.readText() + certificate.readText()) }

        val tls = ChronicleTls.load(connectionString("certificatePath=${encoded(bundle)}"))

        assertFalse(tls.isEmpty)
    }

    @Test
    fun `loads a PEM client certificate with an encrypted key`() {
        val (certificate, key) = generateIdentity("client")
        openssl("pkcs8", "-topk8", "-in", key.name, "-out", "encrypted.key", "-passout", "pass:changeit")
        val encrypted = directory.resolve("encrypted.key").toFile()
        val bundle = directory.resolve("bundle.pem").toFile().apply { writeText(certificate.readText() + encrypted.readText()) }

        assertFalse(ChronicleTls.load(connectionString("certificatePath=${encoded(bundle)}&certificatePassword=changeit")).isEmpty)
        assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("certificatePath=${encoded(bundle)}"))
        }
    }

    @Test
    fun `a PEM key that matches none of the certificates fails`() {
        val (certificate, _) = generateIdentity("client")
        val (_, strangerKey) = generateIdentity("stranger")
        val bundle = directory.resolve("bundle.pem").toFile().apply { writeText(certificate.readText() + strangerKey.readText()) }

        assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("certificatePath=${encoded(bundle)}"))
        }
    }

    @Test
    fun `a missing certificatePath file fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChronicleTls.load(connectionString("certificatePath=%2Fno%2Fsuch.p12"))
        }
    }
}
