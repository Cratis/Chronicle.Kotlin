// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class LeastConnectionsLoadBalancerStrategyTests {

    private val addressA = ChronicleServerAddress("a.example.com", 35000)
    private val addressB = ChronicleServerAddress("b.example.com", 35000)

    private fun response(statusCode: Int, body: String): HttpResponse<String> {
        val response = mockk<HttpResponse<String>>()
        every { response.statusCode() } returns statusCode
        every { response.body() } returns body
        return response
    }

    @Test
    fun `returns the only address without making any HTTP calls`() = runBlocking {
        val httpClient = mockk<HttpClient>()
        val strategy = LeastConnectionsLoadBalancerStrategy(httpClient = httpClient)

        val selected = strategy.select(listOf(addressA))

        assertEquals(addressA, selected)
        verify(exactly = 0) { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) }
    }

    @Test
    fun `selects the address reporting the fewest connections`() = runBlocking {
        val httpClient = mockk<HttpClient>()
        // The strategy probes every address concurrently, so the answer has to read the request it
        // was actually called with. A shared capture slot holds whichever call landed last, which
        // made this stub hand one address's count to the other and fail intermittently.
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } answers {
            val uri = firstArg<HttpRequest>().uri().toString()
            when {
                uri.contains("a.example.com") && uri.endsWith("/connections/count") -> response(200, "20")
                uri.contains("b.example.com") && uri.endsWith("/connections/count") -> response(200, "1")
                uri.endsWith("/connections/reserve") -> response(200, "")
                else -> response(404, "")
            }
        }

        val strategy = LeastConnectionsLoadBalancerStrategy(maxSelectionJitterMs = 0, httpClient = httpClient)
        val selected = strategy.select(listOf(addressA, addressB))

        assertEquals(addressB, selected)
        verify { httpClient.send(match { it.uri().toString().endsWith("/connections/reserve") && it.uri().toString().contains("b.example.com") }, any<HttpResponse.BodyHandler<String>>()) }
    }

    @Test
    fun `treats a failed probe as maximally loaded rather than failing selection`() = runBlocking {
        val httpClient = mockk<HttpClient>()
        every {
            httpClient.send(match { it.uri().toString().contains("a.example.com") && it.uri().toString().endsWith("/connections/count") }, any<HttpResponse.BodyHandler<String>>())
        } throws IOException("connection refused")
        every {
            httpClient.send(match { it.uri().toString().contains("b.example.com") && it.uri().toString().endsWith("/connections/count") }, any<HttpResponse.BodyHandler<String>>())
        } returns response(200, "20")
        every {
            httpClient.send(match { it.uri().toString().endsWith("/connections/reserve") }, any<HttpResponse.BodyHandler<String>>())
        } returns response(200, "")

        val strategy = LeastConnectionsLoadBalancerStrategy(maxSelectionJitterMs = 0, httpClient = httpClient)
        val selected = strategy.select(listOf(addressA, addressB))

        assertEquals(addressB, selected)
    }

    @Test
    fun `probes over plain HTTP when disableTls is true`() = runBlocking {
        val httpClient = mockk<HttpClient>()
        val schemes = java.util.concurrent.ConcurrentLinkedQueue<String>()
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } answers {
            schemes.add(firstArg<HttpRequest>().uri().scheme)
            response(200, "0")
        }

        val strategy = LeastConnectionsLoadBalancerStrategy(disableTls = true, maxSelectionJitterMs = 0, httpClient = httpClient)
        strategy.select(listOf(addressA, addressB))

        // Every probe has to be plain HTTP, not just whichever one happened to land last.
        assertTrue(schemes.isNotEmpty())
        assertTrue(schemes.all { it == "http" })
    }
}
