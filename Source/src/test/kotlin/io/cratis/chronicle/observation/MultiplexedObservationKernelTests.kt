// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@EventType
data class MultiplexedKernelEvent(val value: String)

@ReadModel
data class MultiplexedKernelCount(val count: Int = 0)

object MultiplexedKernelObserved {
    val byReactor = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()
    val reduced = CopyOnWriteArrayList<String>()

    fun note(reactor: String, value: String) {
        byReactor.getOrPut(reactor) { CopyOnWriteArrayList() }.add(value)
    }
}

@Reactor("multiplexed-kernel-reactor-one")
class MultiplexedKernelReactorOne {
    fun received(event: MultiplexedKernelEvent) = MultiplexedKernelObserved.note("one", event.value)
}

@Reactor("multiplexed-kernel-reactor-two")
class MultiplexedKernelReactorTwo {
    fun received(event: MultiplexedKernelEvent) = MultiplexedKernelObserved.note("two", event.value)
}

@Reducer("multiplexed-kernel-reducer")
class MultiplexedKernelReducer {
    @Observes(MultiplexedKernelEvent::class)
    fun reduce(events: EventBatch, current: MultiplexedKernelCount?): MultiplexedKernelCount {
        MultiplexedKernelObserved.reduced += events.map { (it.event as MultiplexedKernelEvent).value }
        return MultiplexedKernelCount((current?.count ?: 0) + events.count())
    }
}

/**
 * Two reactors and a reducer on a real kernel, observed with sharing on and off. Against a kernel that supports sharing
 * the first uses the shared stream; an older kernel serves both per observer. Runs against a Chronicle development kernel; CI supplies the connection string, and a local run skips without it.
 */
class MultiplexedObservationKernelTests {
    private val connectionString: String? = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")

    private fun observeEverything(multiplex: Boolean) = runBlocking {
        assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run against a kernel")
        MultiplexedKernelObserved.byReactor.clear()
        MultiplexedKernelObserved.reduced.clear()

        val options = ChronicleOptions(
            ChronicleConnectionString.parse(connectionString!!),
            programIdentifier = "multiplexed-kernel-tests",
            artifacts = KnownClientArtifacts(
                MultiplexedKernelEvent::class,
                MultiplexedKernelReactorOne::class,
                MultiplexedKernelReactorTwo::class,
                MultiplexedKernelReducer::class
            )
        ).withMultiplexObservation(multiplex)
        ChronicleClient(options).use { client ->
            val store = client.getEventStore("Multiplexed${UUID.randomUUID().toString().replace("-", "").take(12)}")
            store.awaitRegistration()

            val values = (1..5).map { "event-$it" }
            values.forEach { assertTrue(store.eventLog.append(UUID.randomUUID().toString(), MultiplexedKernelEvent(it)).isSuccess) }

            withTimeout(60_000) {
                while (MultiplexedKernelObserved.byReactor["one"]?.size != 5 ||
                    MultiplexedKernelObserved.byReactor["two"]?.size != 5 ||
                    MultiplexedKernelObserved.reduced.size != 5
                ) delay(100)
            }
            assertEquals(values.toSet(), MultiplexedKernelObserved.byReactor["one"]!!.toSet())
            assertEquals(values.toSet(), MultiplexedKernelObserved.byReactor["two"]!!.toSet())
            assertEquals(values.toSet(), MultiplexedKernelObserved.reduced.toSet())
        }
    }

    @Test
    fun `reactors and a reducer observe over the shared stream`() = observeEverything(multiplex = true)

    @Test
    fun `reactors and a reducer observe over a stream each`() = observeEverything(multiplex = false)
}
