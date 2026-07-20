// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.cert.X509Certificate

class InsecureTrustManagerTests {

    private val trustManager = InsecureTrustManager()

    @Test
    fun `accepts any server certificate chain, including an empty one`() {
        assertDoesNotThrow {
            trustManager.checkServerTrusted(emptyArray<X509Certificate>(), "RSA")
        }
    }

    @Test
    fun `accepts any client certificate chain, including an empty one`() {
        assertDoesNotThrow {
            trustManager.checkClientTrusted(emptyArray<X509Certificate>(), "RSA")
        }
    }

    @Test
    fun `has no accepted issuers`() {
        assertTrue(trustManager.acceptedIssuers.isEmpty())
    }

    @Test
    fun `sslContext builds a usable TLS context`() {
        val context = InsecureTrustManager.sslContext()
        assertTrue(context.protocol == "TLS")
    }
}
