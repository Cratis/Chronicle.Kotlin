// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSources.EventSource
import io.cratis.chronicle.eventSources.EventSources
import io.cratis.chronicle.eventSources.EventStream
import io.cratis.chronicle.eventSources.EventStreamDoesNotBelongToEventSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private data class Opened(val id: String)

@EventSource("Account", streams = [EventStream("Transactions")])
private class AccountDefinition

@EventSource("Ledger")
private class LedgerDefinition

class UnitOfWorkEventSourceTests {
    private val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
    private val requests = mutableListOf<Sequences.AppendManyForEventSourcesRequest>()
    private val sequence: IEventSequence
    private val eventStore = mockk<IEventStore>()

    init {
        coEvery { stub.appendManyForEventSources(capture(requests), any()) } answers {
            Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().apply { repeat(requests.last().eventsCount) { addSequenceNumbers(it.toLong()) } })
                .build()
        }
        sequence = EventSequence(
            EventSequenceId.eventLog, "store", "tenant", stub, registrationGate = IRegistrationGate.open,
            eventSources = EventSources("store", null, KnownClientArtifacts(AccountDefinition::class, LedgerDefinition::class))
        )
        every { eventStore.getEventSequence(any()) } returns sequence
    }

    @Test
    fun `staged events carry their definition metadata through commit`() = runBlocking {
        val unitOfWork = UnitOfWork(eventStore = eventStore)
        unitOfWork.addEvent(
            EventSequenceId.eventLog, "acc-1", Opened("1"),
            AppendOptions(eventSource = AccountDefinition::class, eventStream = "Transactions")
        )

        unitOfWork.commit()

        val sent = requests.single().eventsList.single()
        assertEquals("Account", sent.eventSource)
        assertEquals("Account", sent.eventSourceType)
        assertEquals("Transactions", sent.eventStreamType)
    }

    @Test
    fun `events staged for different definitions commit as separate routed batches`() = runBlocking {
        val unitOfWork = UnitOfWork(eventStore = eventStore)
        unitOfWork.addEvent(EventSequenceId.eventLog, "x", Opened("1"), AppendOptions(eventSource = AccountDefinition::class))
        unitOfWork.addEvent(EventSequenceId.eventLog, "x", Opened("2"), AppendOptions(eventSource = LedgerDefinition::class))

        unitOfWork.commit()

        assertEquals(listOf("Account", "Ledger"), requests.map { it.eventsList.single().eventSource })
    }

    @Test
    fun `a routing mistake in a later staged group writes nothing`() {
        val unitOfWork = UnitOfWork(eventStore = eventStore)
        unitOfWork.addEvent(EventSequenceId.eventLog, "x", Opened("1"), AppendOptions(eventSource = AccountDefinition::class))
        unitOfWork.addEvent(
            EventSequenceId.eventLog, "y", Opened("2"),
            AppendOptions(eventSource = AccountDefinition::class, eventStream = "Nope")
        )

        assertThrows<EventStreamDoesNotBelongToEventSource> { runBlocking { unitOfWork.commit() } }

        coVerify(exactly = 0) { stub.appendManyForEventSources(any(), any()) }
    }
}
