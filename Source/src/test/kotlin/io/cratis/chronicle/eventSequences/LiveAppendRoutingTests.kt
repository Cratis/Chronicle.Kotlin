// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventStores.Eventstores
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.connection.ChronicleConnection
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Instant
import java.util.UUID

@EventType
private data class LiveRouteEvent(val marker: String)

/**
 * Appends against a real Chronicle kernel and reads the results back, so that the routing this
 * client no longer decides is proven to be the routing the kernel actually stores.
 *
 * Opt-in: the whole class is skipped unless `CHRONICLE_INTEROP_CONNECTION` names a reachable
 * kernel, for example `chronicle://127.0.0.1:<port>?skipTlsValidation=true`. Each test isolates
 * itself in a freshly ensured event store and namespace, so nothing here depends on run order or
 * on a previous run's leftovers.
 */
@EnabledIfEnvironmentVariable(named = "CHRONICLE_INTEROP_CONNECTION", matches = ".+")
class LiveAppendRoutingTests {
    private lateinit var connection: ChronicleConnection
    private lateinit var store: EventStore
    private lateinit var eventStoreName: String
    private lateinit var namespace: String

    @BeforeEach
    fun establish() = runBlocking {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        eventStoreName = "live-routing-$unique"
        namespace = "ns-$unique"
        connection = ChronicleConnection(ChronicleConnectionString.parse(System.getenv(CONNECTION)))
        connection.connect()

        // getEventStore only hands out a client handle - the server-side store has to be created
        // explicitly, or the first append is rejected with "Event store does not exist."
        val ensured = connection.services.eventStores.ensureEventStore(
            Eventstores.EnsureEventStoreRequest.newBuilder().setName(eventStoreName).build()
        )
        assertEnsured(ensured)

        store = EventStore(eventStoreName, namespace, connection.services, connection.lifecycle)
        store.namespaces.ensure(namespace)
        store.eventTypes.register(LiveRouteEvent::class)
    }

    @AfterEach
    fun destroy() {
        connection.close()
    }

    @Test
    fun `omitted routing is resolved by the kernel rather than by the client`() = live {
        val eventSourceId = "source-default"
        val appended = store.eventLog.append(eventSourceId, LiveRouteEvent("omitted"))
        assertTrue(appended.isSuccess, "append failed: ${appended.errors} ${appended.constraintViolations}")

        val stored = readBack().single()
        assertEquals("Default", stored.context.eventSourceType)
        assertEquals("All", stored.context.eventStreamType)
        assertEquals("Default", stored.context.eventStreamId)
        assertEquals(eventSourceId, stored.context.eventSourceId)
        assertEquals(eventStoreName, stored.context.eventStore)
        assertEquals(namespace, stored.context.namespace)
    }

    @Test
    fun `a read that names no route returns the events the kernel routed by default`() = live {
        val eventSourceId = "source-unnarrowed"
        assertTrue(store.eventLog.append(eventSourceId, LiveRouteEvent("first")).isSuccess)
        assertTrue(store.eventLog.append(eventSourceId, LiveRouteEvent("second")).isSuccess)

        // The reason this is worth proving against a real kernel: nothing here names a route, so an
        // empty filter has to mean "do not narrow" all the way through, or these reads answer with
        // nothing and the events look lost rather than merely filtered out.
        assertEquals(2, readBack().size)
        assertEquals(2, readEventSource(eventSourceId).size)
        assertEquals(1L, store.eventLog.getTailSequenceNumber(eventSourceId).value)

        // The stored route is the kernel's default, so narrowing to it finds them and narrowing to
        // the route this client used to pick silently does not.
        assertEquals(2, readEventSource(eventSourceId, eventStreamType = "All", eventStreamId = "Default").size)
        assertEquals(0, readEventSource(eventSourceId, eventStreamType = "Default").size)
    }

    @Test
    fun `explicit routing subject tags and occurred survive the round trip`() = live {
        val eventSourceId = "source-explicit"
        val occurred = Instant.parse("2020-01-01T00:00:00.123Z")
        val options = AppendOptions(
            eventSourceType = "Account",
            eventStreamType = "Onboarding",
            eventStreamId = "2026",
            subject = "person-42",
            occurred = occurred,
            tags = listOf("live", "routing")
        )
        val appended = store.eventLog.append(eventSourceId, LiveRouteEvent("explicit"), options)
        assertTrue(appended.isSuccess, "append failed: ${appended.errors} ${appended.constraintViolations}")

        val stored = readBack().single()
        assertEquals("Account", stored.context.eventSourceType)
        assertEquals("Onboarding", stored.context.eventStreamType)
        assertEquals("2026", stored.context.eventStreamId)
        assertEquals(eventSourceId, stored.context.eventSourceId)
        // The compliance subject the kernel stored, which the client now carries on the context.
        assertEquals("person-42", stored.context.subject)
        assertEquals(occurred, stored.context.occurred)
        assertEquals(listOf("live", "routing"), stored.context.tags)
        assertEquals("LiveRouteEvent", stored.context.eventType.id.value)
        assertTrue(stored.content.contains("explicit"), "unexpected stored content: ${stored.content}")
    }

    @Test
    fun `a stale concurrency scope is rejected and persists nothing`() = live {
        val eventSourceId = "source-concurrency"
        assertTrue(store.eventLog.append(eventSourceId, LiveRouteEvent("first")).isSuccess)
        assertTrue(store.eventLog.append(eventSourceId, LiveRouteEvent("second")).isSuccess)
        val before = readBack()
        assertEquals(2, before.size)
        val tailBefore = store.eventLog.getTailSequenceNumber(eventSourceId)

        // The source is two events long, so expecting its tail to still be the very first event
        // is exactly the stale read optimistic concurrency exists to reject.
        val stale = AppendOptions(
            concurrencyScope = ConcurrencyScope(EventSequenceNumber.first, eventSourceId = true)
        )
        val rejected = store.eventLog.append(eventSourceId, LiveRouteEvent("stale"), stale)

        assertFalse(rejected.isSuccess, "a stale scope must not append")
        assertNotNull(rejected.concurrencyViolation, "expected a concurrency violation, got $rejected")

        val after = readBack()
        assertEquals(tailBefore, store.eventLog.getTailSequenceNumber(eventSourceId), "the tail moved")
        assertEquals(before.map { it.content }, after.map { it.content })
        assertFalse(after.any { it.content.contains("stale") }, "the rejected event must not be stored")
    }

    /** Reads the whole sequence back, which carries stored routing rather than the request's. */
    private suspend fun readBack(): List<AppendedEvent> =
        store.eventLog.getFromSequenceNumber(EventSequenceNumber.first)

    /** Reads one event source back, optionally narrowed to a stream the way a caller would. */
    private suspend fun readEventSource(
        eventSourceId: String,
        eventStreamType: String? = null,
        eventStreamId: String? = null
    ): List<AppendedEvent> = store.eventLog.getForEventSourceIdAndEventTypes(
        eventSourceId,
        listOf(LiveRouteEvent::class),
        eventStreamType,
        eventStreamId
    )

    private fun assertEnsured(result: Eventstores.CommandResult) {
        assertTrue(result.isAuthorized, "ensuring the event store was not authorized")
        assertEquals(emptyList<String>(), result.validationResultsList.map { it.message })
        assertEquals(emptyList<String>(), result.exceptionMessagesList.toList())
    }

    /**
     * Bounds every live test with a deadline, so an unreachable kernel fails by name instead of
     * hanging the suite. The deadline is a failure boundary, not a wait for anything to happen.
     */
    private fun live(body: suspend () -> Unit) = runBlocking {
        withTimeout(60_000) { body() }
    }

    private companion object {
        const val CONNECTION = "CHRONICLE_INTEROP_CONNECTION"
    }
}
