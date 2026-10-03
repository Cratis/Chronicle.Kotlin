// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.eventSequences.toClient
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.observation.toEventContext
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventSourceMetadataTests {
    @Test
    fun `a persisted event carries the event source name it was appended through`() {
        val response = Sequences.AppendedEventResponse.newBuilder()
            .setContext(Sequences.EventContext.newBuilder().setEventSource("Account")).build()

        assertEquals("Account", response.toClient("store", "tenant").context.eventSource)
    }

    @Test
    fun `an event without registered source metadata has an empty one rather than a fabricated name`() {
        val response = Sequences.AppendedEventResponse.newBuilder()
            .setContext(Sequences.EventContext.newBuilder().setEventSourceType("Default")).build()

        assertEquals("", response.toClient("store", "tenant").context.eventSource)
    }

    @Test
    fun `reactor and reducer delivery carry the event source name`() {
        val reactor = ObservationReactors.EventContext.newBuilder().setEventSource("Account").build()
        val reducer = ObservationReducers.EventContext.newBuilder().setEventSource("Ledger").build()

        assertEquals("Account", reactor.toEventContext().eventSource)
        assertEquals("Ledger", reducer.toEventContext().eventSource)
        assertEquals("", ObservationReactors.EventContext.getDefaultInstance().toEventContext().eventSource)
    }

    @Test
    fun `the pre existing event context constructor still works and leaves the source unset`() {
        val context = EventContext(
            1, "id", EventTypeDescriptor(EventTypeId("E"), EventTypeGeneration(1)), Instant.EPOCH, UUID.randomUUID(),
            Identity.unknown
        )

        assertEquals("", context.eventSource)
    }
}
