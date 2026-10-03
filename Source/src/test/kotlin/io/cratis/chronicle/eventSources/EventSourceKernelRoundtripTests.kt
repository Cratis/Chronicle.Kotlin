// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.eventSequences.appendThroughEventSource
import io.cratis.chronicle.events.EventType
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EventType
data class MoneyDeposited(val amount: Int)

@EventSource(
    "Account",
    description = "A customer account",
    concurrency = [ConcurrencyDimension.EventSourceId],
    streams = [EventStream("Transactions", "Money in and out")]
)
class AccountRoundtripEventSource

/**
 * Round-trips event source definitions through a real kernel. Opt in by pointing
 * `CHRONICLE_KERNEL_PORT` at a Chronicle 19.30.0+ kernel (for example `docker run -p 18300:35000
 * cratis/chronicle:19.30.0-development-slim`); skipped otherwise so the default build needs no kernel.
 */
@EnabledIfEnvironmentVariable(named = "CHRONICLE_KERNEL_PORT", matches = "\\d+")
class EventSourceKernelRoundtripTests {
    @Test
    fun `a definition registers appends and is observed after the server round trip`() = runBlocking {
        val port = System.getenv("CHRONICLE_KERNEL_PORT")
        val options = ChronicleOptions.fromConnectionString(
            "chronicle://chronicle-dev-client:chronicle-dev-secret@localhost:$port"
        ).copy(
            autoDiscoverAndRegister = true,
            artifacts = KnownClientArtifacts(MoneyDeposited::class, AccountRoundtripEventSource::class)
        )

        ChronicleClient(options).use { client ->
            val store = client.getEventStore("eventsource-kotlin-${UUID.randomUUID().toString().take(8)}")
            val log = store.eventLog
            val account = UUID.randomUUID().toString()

            val first = log.appendThroughEventSource(AccountRoundtripEventSource::class, account, MoneyDeposited(5), "Transactions")
            assertTrue(first.isSuccess, first.errors.toString())

            // A plain append in the same store stays free of registered-source metadata.
            val legacy = log.append(UUID.randomUUID().toString(), MoneyDeposited(1))
            assertTrue(legacy.isSuccess)

            val stored = log.getFromSequenceNumber(EventSequenceNumber.first)
            val viaDefinition = stored.single { it.context.eventSourceId == account }.context
            assertEquals("Account", viaDefinition.eventSource)
            assertEquals("Account", viaDefinition.eventSourceType)
            assertEquals("Transactions", viaDefinition.eventStreamType)
            assertEquals("", stored.single { it.context.eventSourceId != account }.context.eventSource)

            val definitions = store.eventSources.all
            assertEquals(listOf("Account"), definitions.map { it.name })
        }
    }

    @Test
    fun `a stale scope on one id rejects the whole mixed batch and appends nothing`() = runBlocking {
        val port = System.getenv("CHRONICLE_KERNEL_PORT")
        val options = ChronicleOptions.fromConnectionString(
            "chronicle://chronicle-dev-client:chronicle-dev-secret@localhost:$port"
        ).copy(
            autoDiscoverAndRegister = true,
            artifacts = KnownClientArtifacts(MoneyDeposited::class, AccountRoundtripEventSource::class)
        )

        ChronicleClient(options).use { client ->
            val store = client.getEventStore("eventsource-kotlin-${UUID.randomUUID().toString().take(8)}")
            val log = store.eventLog
            val guarded = UUID.randomUUID().toString()
            val other = UUID.randomUUID().toString()

            val seeded = log.append(guarded, MoneyDeposited(1))
            assertTrue(seeded.isSuccess, seeded.errors.toString())
            val before = log.getFromSequenceNumber(EventSequenceNumber.first).size

            // Scope expecting the guarded id to still be at sequence number -1 (nothing) - it is stale.
            val stale = ConcurrencyScope(EventSequenceNumber.unavailable, eventSourceId = true, expectsNoMatchingEvent = true)
            val results = runCatching {
                log.appendMany(
                    listOf(
                        EventForEventSourceId(other, MoneyDeposited(2), eventSource = AccountRoundtripEventSource::class),
                        EventForEventSourceId(guarded, MoneyDeposited(3), eventSource = AccountRoundtripEventSource::class)
                    ),
                    mapOf(guarded to stale)
                )
            }

            assertTrue(results.isFailure || results.getOrThrow().any { !it.isSuccess }, "the stale scope must be rejected")
            assertEquals(before, log.getFromSequenceNumber(EventSequenceNumber.first).size, "no entry of the batch may be appended")
        }
    }
}
