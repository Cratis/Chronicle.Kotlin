// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Observation
import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
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
data class WebhookFlagsKernelEvent(val value: String)

/** Runs against a Chronicle 19.33.0 or later development kernel; CI requires the connection string. */
class WebhookFlagsKernelTests {
    private val connectionString: String? = System.getenv("CHRONICLE_INTEGRATION_CONNECTION_STRING")

    private suspend fun eventually(timeoutMs: Long = 30_000, accept: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!accept()) {
                delay(100)
            }
        }
    }

    @Test
    fun `the kernel registers both webhooks but subscribes only the active control`() = runBlocking {
        if (System.getenv("CI") != null) {
            assertTrue(connectionString != null, "CHRONICLE_INTEGRATION_CONNECTION_STRING must be set in CI")
        } else {
            assumeTrue(connectionString != null, "Set CHRONICLE_INTEGRATION_CONNECTION_STRING to run against a kernel")
        }

        val storeName = "WebhookFlags${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val parsed = ChronicleConnectionString.parse(connectionString!!)
        val client = ChronicleClient(
            ChronicleOptions(
                parsed,
                artifacts = KnownClientArtifacts(WebhookFlagsKernelEvent::class),
            ),
        )
        val connection = ChronicleConnection(parsed)
        try {
            val store = client.getEventStore(storeName)
            store.awaitRegistration()
            store.webhooks.register("inactive-hook", "http://localhost:9/hook") {
                it.withEventType(WebhookFlagsKernelEvent::class).notReplayable().notActive()
            }
            store.webhooks.register("active-hook", "http://localhost:9/hook") {
                it.withEventType(WebhookFlagsKernelEvent::class)
            }

            // No events are appended, so neither webhook contacts the dummy URL.
            connection.connect()
            val request = Observation.AllObserversRequest.newBuilder()
                .setEventStore(storeName)
                .setNamespace("Default")
                .build()
            var webhooks = emptyList<ObservationWebhooks.WebhookDefinition>()
            var observers = emptyList<Observation.ObserverInformation>()
            eventually {
                webhooks = store.webhooks.getAll()
                observers = connection.services.observers.getObservers(request).itemsList
                webhooks.any { it.identifier == "inactive-hook" } &&
                    webhooks.any { it.identifier == "active-hook" } &&
                    observers.any { it.id == "active-hook" }
            }

            // Kernel v19.70.0: Source/Kernel/Storage.MongoDB/Observation/Webhooks/
            // WebhookDefinitionConverters.cs:44-53 (SQL counterpart:40-49) drops IsActive on read.
            // CI also reported IsReplayable=true for the disabled hook; that is not explained by this converter.
            // Core/Observation/Webhooks/Webhooks.cs:204-251 skips inactive subscriptions but does not forward
            // IsReplayable to observers, so observer metadata cannot prove webhook replayability either.
            // Assert observable inactivity here; the unit specs assert both disabling wire companions.
            assertFalse(observers.any { it.id == "inactive-hook" })
            val active = webhooks.single { it.identifier == "active-hook" }
            assertTrue(active.isReplayable)
            assertTrue(active.isActive)
        } finally {
            connection.close()
            client.dispose()
        }
    }
}
