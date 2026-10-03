// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import Cratis.Chronicle.Contracts.EventSources.EventSourcesGrpcKt
import Cratis.Chronicle.Contracts.EventSources.Eventsources
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@EventSource(
    description = "A customer account",
    concurrency = [ConcurrencyDimension.EventSourceId],
    streams = [
        EventStream("Transactions", "Money in and out", [ConcurrencyDimension.EventSourceId, ConcurrencyDimension.EventStreamType]),
        EventStream("Settings")
    ]
)
private class AccountEventSource

@EventSource("Account")
private class OtherAccount

@EventSource(streams = [EventStream("Same"), EventStream("Same")])
private class DuplicateStreamEventSource

@EventSource(name = " ")
private class BlankEventSource

private class NotAnEventSource

class EventSourcesTests {
    private fun sources(vararg types: kotlin.reflect.KClass<*>, stub: EventSourcesGrpcKt.EventSourcesCoroutineStub? = null) =
        EventSources("store", stub, KnownClientArtifacts(*types))

    @Test
    fun `derives the name from the class and reads streams and dimensions`() {
        val definition = sources(AccountEventSource::class).getFor(AccountEventSource::class)

        assertEquals("Account", definition.name)
        assertEquals("A customer account", definition.description)
        assertEquals(setOf(ConcurrencyDimension.EventSourceId), definition.concurrency)
        assertEquals(listOf("Transactions", "Settings"), definition.streams.map { it.name })
        assertEquals(
            setOf(ConcurrencyDimension.EventSourceId, ConcurrencyDimension.EventStreamType),
            definition.findStream("Transactions")!!.concurrency
        )
    }

    @Test
    fun `stream concurrency falls back to the event source when it declares none`() {
        val definition = sources(AccountEventSource::class).getFor("Account")

        assertEquals(setOf(ConcurrencyDimension.EventSourceId), definition.concurrencyFor(definition.findStream("Settings")))
        assertEquals(
            setOf(ConcurrencyDimension.EventSourceId, ConcurrencyDimension.EventStreamType),
            definition.concurrencyFor(definition.findStream("Transactions"))
        )
        assertEquals(definition.concurrency, definition.concurrencyFor(null))
    }

    @Test
    fun `rejects two definitions with the same name`() {
        val error = assertThrows<DuplicateEventSourceName> { sources(AccountEventSource::class, OtherAccount::class).all }

        assertEquals("Account", error.name)
    }

    @Test
    fun `rejects a definition that declares a stream twice`() {
        val error = assertThrows<DuplicateEventStreamName> { sources(DuplicateStreamEventSource::class).all }

        assertEquals("Same", error.name)
    }

    @Test
    fun `rejects a blank name and an unannotated class`() {
        assertThrows<InvalidEventSourceDefinition> { EventSources.describe(BlankEventSource::class) }
        assertThrows<InvalidEventSourceDefinition> { EventSources.describe(NotAnEventSource::class) }
    }

    @Test
    fun `an unknown event source is reported by type and by name`() {
        val eventSources = sources(AccountEventSource::class)

        assertThrows<UnknownEventSource> { eventSources.getFor(NotAnEventSource::class) }
        assertThrows<UnknownEventSource> { eventSources.getFor("Nope") }
    }

    @Test
    fun `registers every definition as a client owned upsert`() = runBlocking {
        val request = slot<Eventsources.RegisterEventSourcesRequest>()
        val stub = mockk<EventSourcesGrpcKt.EventSourcesCoroutineStub>()
        coEvery { stub.registerEventSources(capture(request), any()) } returns
            Eventsources.CommandResult.newBuilder().setIsAuthorized(true).build()

        sources(AccountEventSource::class, stub = stub).register()

        assertEquals("store", request.captured.eventStore)
        val source = request.captured.sourcesList.single()
        assertEquals("Account", source.name)
        assertEquals("A customer account", source.description)
        assertEquals(Eventsources.EventSourceOwner.Client, source.owner)
        assertEquals(1, source.concurrencyValue)
        assertEquals(listOf("Transactions", "Settings"), source.streamsList.map { it.name })
        assertEquals("Money in and out", source.streamsList[0].description)
        assertEquals(1 or 4, source.streamsList[0].concurrencyValue)
        assertEquals(0, source.streamsList[1].concurrencyValue)
    }

    @Test
    fun `registers nothing when no definitions are discovered`() = runBlocking {
        val stub = mockk<EventSourcesGrpcKt.EventSourcesCoroutineStub>()

        sources(stub = stub).register()

        coVerify(exactly = 0) { stub.registerEventSources(any(), any()) }
    }

    @Test
    fun `fails registration when the kernel rejects it`() {
        val stub = mockk<EventSourcesGrpcKt.EventSourcesCoroutineStub>()
        coEvery { stub.registerEventSources(any(), any()) } returns
            Eventsources.CommandResult.newBuilder().setIsAuthorized(true).addExceptionMessages("boom").build()

        val error = assertThrows<io.cratis.chronicle.eventSequences.ChronicleCommandRejected> {
            runBlocking { sources(AccountEventSource::class, stub = stub).register() }
        }

        assertTrue(error.message!!.contains("boom"))
    }
}
