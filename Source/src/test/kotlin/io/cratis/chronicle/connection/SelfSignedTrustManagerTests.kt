// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.security.cert.CertPathBuilderException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

// Self-signed certificates for "chronicle-test-host", generated once with keytool. VALID_CERT is
// valid from 2026 to 2036; EXPIRED_CERT is valid from 2020-01-01 to 2020-01-31 (already expired).
private const val VALID_CERT = """-----BEGIN CERTIFICATE-----
MIIDADCCAeigAwIBAgIJAIw+ibHplW1PMA0GCSqGSIb3DQEBCwUAMB4xHDAaBgNV
BAMTE2Nocm9uaWNsZS10ZXN0LWhvc3QwHhcNMjYwNzA5MDkxNjAzWhcNMzYwNzA2
MDkxNjAzWjAeMRwwGgYDVQQDExNjaHJvbmljbGUtdGVzdC1ob3N0MIIBIjANBgkq
hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwihPmr/vNPeWzOigBwxxJE2SRHe4Dc+L
EP5UMyKyykuqhdxtM2kMkIrGx9GODAE37b55Lon9p/zX5Gg9pgbEqFcUyQzx6tnB
k7OGWLWgV4T4mhiJXIw0dqANN6X1i5Ej/vrrld3XFi0Z2aty2EkyjSBQGoycum3S
Y2c6MLvHbRmg/xPsOLXVKnAEecnHs41i2hJvaSlqmr6EJ+SQa6EbGWRBIdbVi4zG
QpqQaBnUjKCiNMqzekxzIbjnbO4MMZWrwzKYO8P5zxloi/OruJJDo7HeOm6t6Gk6
lXvil0yoIvifSjvWFjU6fdIV4qeeVKHFgZEfD6unZlx7wlM5m8t66wIDAQABo0Ew
PzAdBgNVHQ4EFgQUCjr2Fj+iY9Hm/IPjH6UyIMMCo0kwHgYDVR0RBBcwFYITY2hy
b25pY2xlLXRlc3QtaG9zdDANBgkqhkiG9w0BAQsFAAOCAQEAgsca09XS4BBLIXv0
yswDyFB+yywey/0FKjUa02fUKYTrVC92f6jYaLC8ENS2+YZnvktXih+L3AADT+kt
3SC4iBsIcdS397zB301DR9e1GfLGAXQPN8Q0MQy87/O1gt3XP+5C6ifADwKg84yC
UDFIo3eLbevQIyf0E4tHKVm5r0A6Dqw4nhC4Y9OLOOUM4d/IAPudqVHdzjhB8uGJ
HEgREyOQ/PncVTdxf/zE0r0s0ed+AAtxvLBc1kuQ+5nsImbI4UXFZsyYlG+63e4i
w7cLwtFJNw46MbxKurAi1PIBqsLpV04ZsQzBLwRQENVw5Jc8M4Ds7vrHOFMfHyGP
pO2BFQ==
-----END CERTIFICATE-----"""

private const val EXPIRED_CERT = """-----BEGIN CERTIFICATE-----
MIIDADCCAeigAwIBAgIJAPsVuaq+qAg+MA0GCSqGSIb3DQEBCwUAMB4xHDAaBgNV
BAMTE2Nocm9uaWNsZS10ZXN0LWhvc3QwHhcNMTkxMjMxMjMwMDAwWhcNMjAwMTMw
MjMwMDAwWjAeMRwwGgYDVQQDExNjaHJvbmljbGUtdGVzdC1ob3N0MIIBIjANBgkq
hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7BMnVZi6Nlf5yu/WaHjhO6EeoSfHUo3g
d4KaGxnMc14Kc2kdGCFK0VTIK9BXxE0sBYm3XEiK0FsaMDwifB2C1F7QIZzZJPB6
vBSHjARmBs5t3KWsm0f9HJiqQiJzKpXiHm6ajlDtYh+KkHaqkRmyQo3qz69KPDLk
KHtb/MnELCp1dQf2yYEmw0Dij4/Ftd950iFSsy5kgGsd7fNxb3b9YSH4lcyuexD3
NC1Ffbdaq5bZNATaSM5gEFhWItM7xmJLKaZnayLrH8tgCrqTkOyvYtGNfJGRsKUa
AxrMbrl8/9QF6Aj4SM3RsccPfsh3KajLCPTX+au8OwCn279h4libXwIDAQABo0Ew
PzAdBgNVHQ4EFgQUqeoxfr6tS5Sr4cqHLe5d3bFF2MIwHgYDVR0RBBcwFYITY2hy
b25pY2xlLXRlc3QtaG9zdDANBgkqhkiG9w0BAQsFAAOCAQEAcUCDRrBdUNhDEPsm
/7CqbSsSH+SbQGr5KXDAE1B1WP0d8m8+VRMBfGDbLO1dvdon/rE5+OhXGwjSiWqq
AIPzxJbaB0mJ2SBLuZcIM1y2+woi+HexogWeFlvjnRrscJxN7vcTRVvbKOzrUbYz
rfCaNdbh5UGixQ7jLWrYSn6rEvkfeQV+DIycM+zDrs9wlQl572hfuuK9bY6iRjPi
nQwvd5BJF+3I3PmoioGie5Gwn0KOWNr00yf/S8s4rSxcN9+ZgruGSLaOY2YcFDBQ
Te9wNdkAfNbtC5o2xCY9/Iiza9COKXfOhGNImIELWUjmaPTVsnR4ojngFmj1UPUY
45OK+g==
-----END CERTIFICATE-----"""

private fun certificateFrom(pem: String): X509Certificate =
    CertificateFactory.getInstance("X.509")
        .generateCertificate(ByteArrayInputStream(pem.toByteArray())) as X509Certificate

private val untrustedRootFailure: CertificateException
    get() = CertificateException(
        "PKIX path building failed",
        CertPathBuilderException("unable to find valid certification path to requested target")
    )

class SelfSignedTrustManagerTests {

    private val validCert = certificateFrom(VALID_CERT)
    private val expiredCert = certificateFrom(EXPIRED_CERT)

    private fun trustManager(expectedHost: String, delegate: X509TrustManager) =
        SelfSignedTrustManager(expectedHost, delegate)

    @Test
    fun `accepts a chain that is trusted and whose host matches`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } returns Unit

        assertDoesNotThrow {
            trustManager("chronicle-test-host", delegate).checkServerTrusted(arrayOf(validCert), "RSA")
        }
    }

    @Test
    fun `tolerates a chain that fails only because the root is self-signed and untrusted`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } throws untrustedRootFailure

        assertDoesNotThrow {
            trustManager("chronicle-test-host", delegate).checkServerTrusted(arrayOf(validCert), "RSA")
        }
    }

    @Test
    fun `rejects a self-signed certificate that has also expired`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } throws untrustedRootFailure

        assertThrows(CertificateException::class.java) {
            trustManager("chronicle-test-host", delegate).checkServerTrusted(arrayOf(expiredCert), "RSA")
        }
    }

    @Test
    fun `rejects a chain failure that is not a trust-anchor failure`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } throws CertificateException("revoked")

        assertThrows(CertificateException::class.java) {
            trustManager("chronicle-test-host", delegate).checkServerTrusted(arrayOf(validCert), "RSA")
        }
    }

    @Test
    fun `tolerates a hostname mismatch when the chain is otherwise trusted`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } returns Unit

        assertDoesNotThrow {
            trustManager("some-other-host", delegate).checkServerTrusted(arrayOf(validCert), "RSA")
        }
    }

    @Test
    fun `rejects when both the chain fails and the hostname does not match`() {
        val delegate = mockk<X509TrustManager>()
        every { delegate.checkServerTrusted(any(), any()) } throws untrustedRootFailure

        assertThrows(CertificateException::class.java) {
            trustManager("some-other-host", delegate).checkServerTrusted(arrayOf(validCert), "RSA")
        }
    }
}
