// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import io.cratis.chronicle.artifacts.KnownClientArtifacts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@EventSource("Cart", streams = [EventStream("Items"), EventStream("Payment", concurrency = [ConcurrencyDimension.EventStreamType])])
private class CartEventSource

class ResolvedEventRoutingTests {
    private val eventSources = EventSources("store", null, KnownClientArtifacts(CartEventSource::class))

    private fun resolve(stream: String? = null, source: String? = null, streamType: String? = null) =
        ResolvedEventRouting.resolve(eventSources, CartEventSource::class, stream, source, streamType)

    @Test
    fun `resolves the source without a stream`() {
        val routing = resolve()

        assertEquals("Cart", routing.eventSourceType)
        assertNull(routing.eventStreamType)
    }

    @Test
    fun `resolves a declared stream`() {
        assertEquals("Items", resolve(stream = "Items").eventStreamType)
        assertEquals(setOf(ConcurrencyDimension.EventStreamType), resolve(stream = "Payment").dimensions)
    }

    @Test
    fun `rejects an undeclared stream`() {
        assertThrows<EventStreamDoesNotBelongToEventSource> { resolve(stream = "Nope") }
    }

    @Test
    fun `rejects an explicit event source type that contradicts the definition`() {
        assertThrows<EventRoutingContradictsEventSource> { resolve(source = "Other") }
    }

    @Test
    fun `accepts an explicit event source type that matches or is unspecified`() {
        for (source in listOf("Cart", "Default", "")) assertEquals("Cart", resolve(source = source).eventSourceType)
    }

    @Test
    fun `rejects an explicit stream type that contradicts the named stream`() {
        assertThrows<EventRoutingContradictsEventSource> { resolve(stream = "Items", streamType = "Payment") }
    }

    @Test
    fun `an explicit stream type alone selects the stream and must be declared`() {
        assertEquals("Payment", resolve(streamType = "Payment").eventStreamType)
        assertThrows<EventStreamDoesNotBelongToEventSource> { resolve(streamType = "Bogus") }
        assertNull(resolve(streamType = "All").eventStreamType)
    }

    @Test
    fun `an unknown event source is rejected`() {
        assertThrows<UnknownEventSource> {
            ResolvedEventRouting.resolve(eventSources, String::class, null, null, null)
        }
    }
}
