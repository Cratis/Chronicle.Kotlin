// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.eventSources.ConcurrencyDimension
import io.cratis.chronicle.eventSources.ConflictingEventSourceConcurrency
import io.cratis.chronicle.eventSources.EventRoutingContradictsEventSource
import io.cratis.chronicle.eventSources.EventSource
import io.cratis.chronicle.eventSources.EventSources
import io.cratis.chronicle.eventSources.EventStream
import io.cratis.chronicle.eventSources.EventStreamDoesNotBelongToEventSource
import io.cratis.chronicle.eventSources.UnknownEventSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private data class Deposited(val amount: Int)

@EventSource(
    "Account",
    concurrency = [ConcurrencyDimension.EventSourceId],
    streams = [
        EventStream("Transactions", concurrency = [ConcurrencyDimension.EventSourceId, ConcurrencyDimension.EventStreamType]),
        EventStream("Settings")
    ]
)
private class AccountEventSource

@EventSource("Ledger")
private class LedgerEventSource

class EventSourceAppendTests {
    private val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
    private val append = slot<Sequences.AppendRequest>()
    private val appendMany = slot<Sequences.AppendManyForEventSourcesRequest>()
    private val tails = mutableListOf<Sequences.TailSequenceNumberRequest>()
    private var tail = -1L
    private val sequence: EventSequence

    init {
        coEvery { stub.append(capture(append), any()) } returns Sequences.CommandResult_AppendResponse.newBuilder()
            .setIsAuthorized(true).setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(3)).build()
        coEvery { stub.appendManyForEventSources(capture(appendMany), any()) } answers {
            val count = appendMany.captured.eventsCount
            Sequences.CommandResult_AppendManyResponse.newBuilder().setIsAuthorized(true)
                .setResponse(Sequences.AppendManyResponse.newBuilder().apply { repeat(count) { addSequenceNumbers(it.toLong()) } }).build()
        }
        coEvery { stub.tailSequenceNumber(capture(tails), any()) } answers {
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder().setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(tail)).build()
        }
        sequence = EventSequence(
            EventSequenceId.eventLog, "store", "tenant", stub,
            eventSources = EventSources("store", null, KnownClientArtifacts(AccountEventSource::class, LedgerEventSource::class))
        )
    }

    @Test
    fun `appends through a definition stamping source type stream and name`() = runBlocking {
        sequence.appendThroughEventSource(AccountEventSource::class, "acc-1", Deposited(5), "Settings")

        assertEquals("Account", append.captured.eventSource)
        assertEquals("Account", append.captured.eventSourceType)
        assertEquals("Settings", append.captured.eventStreamType)
        assertEquals("acc-1", append.captured.eventSourceId)
    }

    @Test
    fun `source level dimensions give a scope over the event source id from the tail`() = runBlocking {
        tail = 41

        sequence.appendThroughEventSource(AccountEventSource::class, "acc-1", Deposited(5), "Settings")

        val scope = append.captured.concurrencyScope
        assertEquals(41, scope.sequenceNumber)
        assertTrue(scope.eventSourceId)
        assertEquals("", scope.eventStreamType)
        assertEquals("acc-1", tails.single().eventSourceId)
    }

    @Test
    fun `stream dimensions override the source dimensions`() = runBlocking {
        tail = 7

        sequence.appendThroughEventSource(AccountEventSource::class, "acc-1", Deposited(5), "Transactions")

        val scope = append.captured.concurrencyScope
        assertTrue(scope.eventSourceId)
        assertEquals("Transactions", scope.eventStreamType)
        assertEquals("Transactions", tails.single().eventStreamType)
    }

    @Test
    fun `an empty matching set expects no matching event`() = runBlocking {
        tail = -1

        sequence.appendThroughEventSource(AccountEventSource::class, "acc-1", Deposited(5))

        assertTrue(append.captured.concurrencyScope.expectsNoMatchingEvent)
    }

    @Test
    fun `a definition without dimensions leaves the append unchecked like a plain append`() = runBlocking {
        sequence.appendThroughEventSource(LedgerEventSource::class, "led-1", Deposited(5))

        assertEquals("Ledger", append.captured.eventSource)
        assertTrue(tails.isEmpty())
        assertEquals(ConcurrencyScope.none.sequenceNumber.value, append.captured.concurrencyScope.sequenceNumber)
        assertFalse(append.captured.concurrencyScope.eventSourceId)
    }

    @Test
    fun `an explicit concurrency scope wins over the definition`() = runBlocking {
        val explicit = ConcurrencyScope(EventSequenceNumber(99), eventSourceId = false, eventStreamType = "Custom")

        sequence.appendThroughEventSource(
            AccountEventSource::class, "acc-1", Deposited(5), "Transactions", AppendOptions(concurrencyScope = explicit)
        )

        assertEquals(99, append.captured.concurrencyScope.sequenceNumber)
        assertEquals("Custom", append.captured.concurrencyScope.eventStreamType)
        assertFalse(append.captured.concurrencyScope.eventSourceId)
        assertTrue(tails.isEmpty())
    }

    @Test
    fun `rejects unknown sources undeclared streams and contradictory routing without sending`() {
        assertThrows<UnknownEventSource> { runBlocking { sequence.appendThroughEventSource(String::class, "x", Deposited(1)) } }
        assertThrows<EventStreamDoesNotBelongToEventSource> {
            runBlocking { sequence.appendThroughEventSource(AccountEventSource::class, "x", Deposited(1), "Nope") }
        }
        assertThrows<EventRoutingContradictsEventSource> {
            runBlocking {
                sequence.appendThroughEventSource(
                    AccountEventSource::class, "x", Deposited(1), options = AppendOptions(eventSourceType = "Other")
                )
            }
        }
        assertThrows<IllegalArgumentException> {
            runBlocking { sequence.append("x", Deposited(1), AppendOptions(eventStream = "Settings")) }
        }
        coVerify(exactly = 0) { stub.append(any(), any()) }
    }

    @Test
    fun `append many through a definition is one atomic routed batch`() = runBlocking {
        tail = 10

        sequence.appendManyThroughEventSource(AccountEventSource::class, "acc-1", listOf(Deposited(1), Deposited(2)), "Transactions")

        val sent = appendMany.captured
        assertEquals(2, sent.eventsCount)
        sent.eventsList.forEach {
            assertEquals("Account", it.eventSource)
            assertEquals("Account", it.eventSourceType)
            assertEquals("Transactions", it.eventStreamType)
        }
        val scope = sent.concurrencyScopesList.single()
        assertEquals("acc-1", scope.eventSourceId)
        assertEquals(10, scope.scope.sequenceNumber)
        coVerify(exactly = 1) { stub.appendManyForEventSources(any(), any()) }
        coVerify(exactly = 0) { stub.appendMany(any(), any()) }
    }

    @Test
    fun `mixed source batches route per event and explicit scopes take precedence`() = runBlocking {
        tail = 5
        val explicit = ConcurrencyScope(EventSequenceNumber(77), eventSourceId = true)

        sequence.appendMany(
            listOf(
                EventForEventSourceId("acc-1", Deposited(1), eventSource = AccountEventSource::class, eventStream = "Transactions"),
                EventForEventSourceId("led-1", Deposited(2), eventSource = LedgerEventSource::class),
                EventForEventSourceId("acc-2", Deposited(3), eventSource = AccountEventSource::class),
                EventForEventSourceId("plain-1", Deposited(4))
            ),
            mapOf("acc-2" to explicit)
        )

        val events = appendMany.captured.eventsList
        assertEquals(listOf("Account", "Ledger", "Account", ""), events.map { it.eventSource })
        assertEquals(listOf("Account", "Ledger", "Account", ""), events.map { it.eventSourceType })
        val scopes = appendMany.captured.concurrencyScopesList.associate { it.eventSourceId to it.scope }
        assertEquals(setOf("acc-1", "acc-2"), scopes.keys)
        assertEquals(5, scopes["acc-1"]!!.sequenceNumber)
        assertEquals(77, scopes["acc-2"]!!.sequenceNumber)
    }

    @Test
    fun `entries sharing an id but deriving different scopes are rejected before anything is sent`() {
        tail = 5

        val failure = assertThrows<ConflictingEventSourceConcurrency> {
            runBlocking {
                sequence.appendMany(
                    listOf(
                        EventForEventSourceId("shared", Deposited(1), eventSource = AccountEventSource::class, eventStream = "Transactions"),
                        EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class)
                    )
                )
            }
        }

        assertEquals("shared", failure.eventSourceId)
        coVerify(exactly = 0) { stub.appendManyForEventSources(any(), any()) }
    }

    @Test
    fun `a conflict is detected whichever entry comes first`() {
        tail = 5

        assertThrows<ConflictingEventSourceConcurrency> {
            runBlocking {
                sequence.appendMany(
                    listOf(
                        EventForEventSourceId("shared", Deposited(1), eventSource = AccountEventSource::class),
                        EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class, eventStream = "Transactions")
                    )
                )
            }
        }
        coVerify(exactly = 0) { stub.appendManyForEventSources(any(), any()) }
    }

    @Test
    fun `an unguarded entry sharing an id with a guarded one never suppresses the guard in either order`() = runBlocking {
        tail = 5
        val unguarded = EventForEventSourceId("shared", Deposited(1), eventSource = LedgerEventSource::class)
        val guarded = EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class)

        sequence.appendMany(listOf(unguarded, guarded))
        sequence.appendMany(listOf(guarded, unguarded))

        // The captured slot holds the last request; both orders must have produced the same guard.
        val scope = appendMany.captured.concurrencyScopesList.single()
        assertEquals("shared", scope.eventSourceId)
        assertEquals(5, scope.scope.sequenceNumber)
        coVerify(exactly = 2) { stub.appendManyForEventSources(any(), any()) }
    }

    @Test
    fun `equivalent predicates keep the first guard when the tail moves between derivations`() = runBlocking {
        tail = 5
        coEvery { stub.tailSequenceNumber(capture(tails), any()) } answers {
            Sequences.QueryResult_EventSequenceTailResponse.newBuilder().setIsAuthorized(true)
                .setData(Sequences.EventSequenceTailResponse.newBuilder().setSequenceNumber(tail++)).build()
        }

        sequence.appendMany(
            listOf(
                EventForEventSourceId("shared", Deposited(1), eventSource = AccountEventSource::class),
                EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class, eventStream = "Settings"),
                EventForEventSourceId("shared", Deposited(3), eventSource = AccountEventSource::class)
            )
        )

        assertEquals(5, appendMany.captured.concurrencyScopesList.single().scope.sequenceNumber)
    }

    @Test
    fun `entries sharing an id that agree on the derived scope send it once`() = runBlocking {
        tail = 5

        sequence.appendMany(
            listOf(
                EventForEventSourceId("shared", Deposited(1), eventSource = AccountEventSource::class),
                EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class)
            )
        )

        val scopes = appendMany.captured.concurrencyScopesList
        assertEquals(1, scopes.size)
        assertEquals("shared", scopes.single().eventSourceId)
        assertEquals(5, scopes.single().scope.sequenceNumber)
    }

    @Test
    fun `an explicit scope for the shared id settles the conflict and wins`() = runBlocking {
        tail = 5
        val explicit = ConcurrencyScope(EventSequenceNumber(77), eventSourceId = true)

        sequence.appendMany(
            listOf(
                EventForEventSourceId("shared", Deposited(1), eventSource = AccountEventSource::class, eventStream = "Transactions"),
                EventForEventSourceId("shared", Deposited(2), eventSource = AccountEventSource::class)
            ),
            mapOf("shared" to explicit)
        )

        assertEquals(77, appendMany.captured.concurrencyScopesList.single().scope.sequenceNumber)
    }

    @Test
    fun `different ids keep their own derived scopes per entry`() = runBlocking {
        tail = 5

        sequence.appendMany(
            listOf(
                EventForEventSourceId("one", Deposited(1), eventSource = AccountEventSource::class, eventStream = "Transactions"),
                EventForEventSourceId("two", Deposited(2), eventSource = AccountEventSource::class)
            )
        )

        val scopes = appendMany.captured.concurrencyScopesList.associate { it.eventSourceId to it.scope }
        assertTrue(scopes["one"]!!.eventStreamType.isNotEmpty())
        assertTrue(scopes["two"]!!.eventStreamType.isEmpty())
    }

    @Test
    fun `a mismatched stream in any event fails the whole batch before sending`() {
        assertThrows<EventStreamDoesNotBelongToEventSource> {
            runBlocking {
                sequence.appendMany(
                    listOf(
                        EventForEventSourceId("acc-1", Deposited(1), eventSource = AccountEventSource::class),
                        EventForEventSourceId("acc-1", Deposited(2), eventSource = AccountEventSource::class, eventStream = "Nope")
                    )
                )
            }
        }
        coVerify(exactly = 0) { stub.appendManyForEventSources(any(), any()) }
    }

    @Test
    fun `plain appends do not carry an event source`() = runBlocking {
        sequence.append("acc-1", Deposited(5))
        sequence.appendMany(listOf(EventForEventSourceId("acc-1", Deposited(5))))

        assertEquals("", append.captured.eventSource)
        assertEquals("", appendMany.captured.eventsList.single().eventSource)
        assertTrue(tails.isEmpty())
    }

    @Test
    fun `append operations report the event source name`() = runBlocking {
        val emitted = mutableListOf<List<AppendedEventWithResult>>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { sequence.appendOperations.collect { emitted += it } }

        sequence.appendThroughEventSource(LedgerEventSource::class, "led-1", Deposited(1))
        sequence.append("led-2", Deposited(2))
        job.cancel()

        assertEquals(listOf("Ledger", ""), emitted.map { it.single().context.eventSource })
    }
}
