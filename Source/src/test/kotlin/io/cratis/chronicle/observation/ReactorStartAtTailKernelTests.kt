// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@EventType
data class SomethingHappenedBeforeRegistration(val name: String)

// The client constructs reactor instances; share sinks for assertions across that boundary.
private val fromTheTail = CopyOnWriteArrayList<String>()
private val fromTheBeginning = CopyOnWriteArrayList<String>()

@Reactor("start-at-tail-reactor")
@StartAtTail
class KernelTailReactor {
    fun received(event: SomethingHappenedBeforeRegistration) {
        fromTheTail.add(event.name)
    }
}

// The control: the same registration without the annotation catches up on all history.
@Reactor("history-reactor")
class KernelHistoryReactor {
    fun received(event: SomethingHappenedBeforeRegistration) {
        fromTheBeginning.add(event.name)
    }
}

/**
 * Runs against a real Chronicle kernel. Opt in by pointing `CHRONICLE_INTEGRATION_CONNECTION_STRING`
 * at a development kernel of 19.70.0 or later, for example
 * `chronicle://chronicle-dev-client:chronicle-dev-secret@localhost:35000`. In CI
 * (`CI` set) a missing connection string fails instead of skipping, so it cannot pass vacuously.
 */
class ReactorStartAtTailKernelTests {

    private val connectionString: String? = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")

    private suspend fun eventually(timeoutMs: Long = 30_000, accept: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!accept()) {
                delay(100)
            }
        }
    }

    @Test
    fun `a reactor marked to start at the tail skips history while an unmarked one catches up on it`() = runBlocking {
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI")
        } else {
            assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run against a kernel")
        }

        val storeName = "StartAtTail${UUID.randomUUID().toString().replace("-", "").take(12)}"
        fun optionsFor(vararg artifacts: kotlin.reflect.KClass<*>) =
            ChronicleOptions(ChronicleConnectionString.parse(connectionString!!), artifacts = KnownClientArtifacts(*artifacts))

        // History exists before either reactor has ever been registered.
        val writerClient = ChronicleClient(optionsFor(SomethingHappenedBeforeRegistration::class))
        val readerClient = ChronicleClient(
            optionsFor(SomethingHappenedBeforeRegistration::class, KernelTailReactor::class, KernelHistoryReactor::class)
        )
        try {
            val writer = writerClient.getEventStore(storeName)
            writer.awaitRegistration()
            for (name in listOf("one", "two", "three")) {
                writer.eventLog.append(UUID.randomUUID().toString(), SomethingHappenedBeforeRegistration(name))
            }

            val reader = readerClient.getEventStore(storeName)
            reader.awaitRegistration()

            // The control proves the history was deliverable, so the tail reactor's silence is not vacuous.
            eventually { fromTheBeginning.size == 3 }

            reader.eventLog.append(UUID.randomUUID().toString(), SomethingHappenedBeforeRegistration("after"))
            eventually { fromTheTail.size == 1 && fromTheBeginning.size == 4 }

            assertFalse(fromTheTail.any { it in listOf("one", "two", "three") })
            assertEquals(listOf("after"), fromTheTail.toList())
            // Each event has its own event source, so the kernel may deliver the partitions in any order.
            assertEquals(listOf("after", "one", "three", "two"), fromTheBeginning.sorted())
        } finally {
            writerClient.dispose()
            readerClient.dispose()
        }
    }
}
