// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Observation
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnection
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@EventType
data class OnceOnlyKernelEvent(val value: String)

@Reactor("once-only-kernel-reactor")
@OnceOnly
class KernelOnceOnlyReactor {
    fun received(event: OnceOnlyKernelEvent) = Unit
}

@Reactor("replayable-kernel-reactor")
class KernelReplayableReactor {
    fun received(event: OnceOnlyKernelEvent) = Unit
}

/** Runs against a Chronicle 19.33.0 or later development kernel; CI requires the connection string. */
class OnceOnlyReactorKernelTests {
    private val connectionString: String? = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")

    private suspend fun eventually(timeoutMs: Long = 30_000, accept: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!accept()) {
                delay(100)
            }
        }
    }

    @Test
    fun `the kernel registers a class-level OnceOnly reactor as non-replayable and its control as replayable`() = runBlocking {
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI")
        } else {
            assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run against a kernel")
        }

        val storeName = "OnceOnly${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val parsed = ChronicleConnectionString.parse(connectionString!!)
        val client = ChronicleClient(
            ChronicleOptions(
                parsed,
                artifacts = KnownClientArtifacts(
                    OnceOnlyKernelEvent::class,
                    KernelOnceOnlyReactor::class,
                    KernelReplayableReactor::class,
                ),
            ),
        )
        val connection = ChronicleConnection(parsed)
        try {
            client.getEventStore(storeName).awaitRegistration()
            connection.connect()
            val request = Observation.AllObserversRequest.newBuilder()
                .setEventStore(storeName)
                .setNamespace("Default")
                .build()
            var observers = emptyList<Observation.ObserverInformation>()
            eventually {
                observers = connection.services.observers.getObservers(request).itemsList
                observers.any { it.id == "once-only-kernel-reactor" } &&
                    observers.any { it.id == "replayable-kernel-reactor" }
            }

            assertFalse(observers.single { it.id == "once-only-kernel-reactor" }.isReplayable)
            assertTrue(observers.single { it.id == "replayable-kernel-reactor" }.isReplayable)
        } finally {
            connection.close()
            client.dispose()
        }
    }
}
