// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventStores.EventStoresGrpcKt
import Cratis.Chronicle.Contracts.EventStores.Eventstores
import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.Events.Events
import Cratis.Chronicle.Contracts.Namespaces.NamespacesGrpcKt
import Cratis.Chronicle.Contracts.Namespaces.NamespacesOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.java.BlockingEventStore
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.InsecureServerCredentials
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import java.util.Collections
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType(id = "ProvisioningLifecycleEvent")
private data class ProvisioningLifecycleEvent(val marker: String = "marker")

class EventStoreTests {
    private var server: Server? = null
    private var channel: ManagedChannel? = null

    @AfterEach
    fun stopServer() {
        channel?.shutdownNow()
        server?.shutdownNow()
    }

    private fun newEventStore(): EventStore {
        val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()
        return EventStore("my-store", "default", ChronicleServices(channel), ConnectionLifecycle())
    }

    @Test
    fun `getEventSequence returns an event sequence with the requested id`() {
        val store = newEventStore()
        val id = EventSequenceId("some-other-sequence")

        val sequence = store.getEventSequence(id)

        assertEquals(id, sequence.id)
    }

    @Test
    fun `getEventSequence returns the same cached instance for the same id`() {
        val store = newEventStore()
        val id = EventSequenceId("some-other-sequence")

        val first = store.getEventSequence(id)
        val second = store.getEventSequence(id)

        assertSame(first, second)
    }

    @Test
    fun `getEventSequence returns different instances for different ids`() {
        val store = newEventStore()

        val first = store.getEventSequence(EventSequenceId("sequence-one"))
        val second = store.getEventSequence(EventSequenceId("sequence-two"))

        assertNotSame(first, second)
    }

    @Test
    fun `awaitRegistration returns straight away when automatic registration is turned off`() = runTest {
        withTimeout(1_000) { newEventStore().awaitRegistration() }
    }

    @Test
    fun `first append ensures store then namespace then registers event types before append`() = runTest {
        val kernel = startKernel()
        val lifecycle = connectedLifecycle()
        val store = eventStore(kernel, lifecycle)

        store.eventLog.append("source", ProvisioningLifecycleEvent())

        assertEquals(
            listOf("EnsureEventStore", "EnsureNamespace:tenant-a", "EventTypes.Register", "Append:tenant-a"),
            kernel.operations
        )
    }

    @Test
    fun `concurrent first appends and registration share one generation pass`() = runTest {
        val kernel = startKernel().apply { registrationDelayMillis = 100 }
        val lifecycle = connectedLifecycle()
        val store = eventStore(kernel, lifecycle)

        coroutineScope {
            val appends = List(20) { index ->
                async { store.eventLog.append("source-$index", ProvisioningLifecycleEvent("event-$index")) }
            }
            val registration = async { store.registerAll() }
            appends.awaitAll()
            registration.await()
        }

        assertEquals(1, kernel.times("EnsureEventStore"))
        assertEquals(1, kernel.times("EnsureNamespace:tenant-a"))
        assertEquals(1, kernel.times("EventTypes.Register"))
        assertEquals(20, kernel.operations.count { it == "Append:tenant-a" })
        assertTrue(kernel.operations.indexOf("EventTypes.Register") < kernel.operations.indexOf("Append:tenant-a"))
    }

    @Test
    fun `store created after connection registers exactly once`() = runTest {
        val kernel = startKernel()
        val lifecycle = connectedLifecycle()

        val store = eventStore(kernel, lifecycle)
        store.awaitRegistration()

        assertEquals(1, kernel.times("EnsureEventStore"))
        assertEquals(1, kernel.times("EnsureNamespace:tenant-a"))
        assertEquals(1, kernel.times("EventTypes.Register"))
    }

    @Test
    fun `reconnect reprovisions before reregistering and appending`() = runTest {
        val kernel = startKernel()
        val lifecycle = connectedLifecycle()
        val store = eventStore(kernel, lifecycle)
        store.awaitRegistration()

        lifecycle.markDisconnected()
        lifecycle.markConnected("connection-2")
        store.eventLog.append("after-reconnect", ProvisioningLifecycleEvent())

        assertEquals(
            listOf(
                "EnsureEventStore",
                "EnsureNamespace:tenant-a",
                "EventTypes.Register",
                "EnsureEventStore",
                "EnsureNamespace:tenant-a",
                "EventTypes.Register",
                "Append:tenant-a"
            ),
            kernel.operations
        )
    }

    @Test
    fun `authorization failure blocks append`() {
        val kernel = startKernel().apply {
            eventStoreResult = Eventstores.CommandResult.newBuilder()
                .setIsAuthorized(false)
                .setAuthorizationFailureReason("denied")
                .build()
        }
        val store = eventStore(kernel, connectedLifecycle())

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking { store.eventLog.append("source", ProvisioningLifecycleEvent()) }
        }

        assertTrue(exception.message.orEmpty().contains("denied"))
        assertEquals(0, kernel.operations.count { it.startsWith("Append:") })
    }

    @Test
    fun `validation failure blocks append`() {
        val kernel = startKernel().apply {
            eventStoreResult = Eventstores.CommandResult.newBuilder()
                .addValidationResults(Eventstores.ValidationResult.newBuilder().setMessage("invalid store"))
                .build()
        }
        val store = eventStore(kernel, connectedLifecycle())

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking { store.eventLog.append("source", ProvisioningLifecycleEvent()) }
        }

        assertTrue(exception.message.orEmpty().contains("invalid store"))
        assertEquals(0, kernel.operations.count { it.startsWith("Append:") })
    }

    @Test
    fun `kernel exception result blocks append`() {
        val kernel = startKernel().apply {
            eventStoreResult = Eventstores.CommandResult.newBuilder()
                .addExceptionMessages("store exploded")
                .build()
        }
        val store = eventStore(kernel, connectedLifecycle())

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking { store.eventLog.append("source", ProvisioningLifecycleEvent()) }
        }

        assertTrue(exception.message.orEmpty().contains("store exploded"))
        assertEquals(0, kernel.operations.count { it.startsWith("Append:") })
    }

    @Test
    fun `namespace command failure blocks registration and append`() {
        val kernel = startKernel().apply {
            namespaceResult = NamespacesOuterClass.CommandResult.newBuilder()
                .addValidationResults(NamespacesOuterClass.ValidationResult.newBuilder().setMessage("invalid namespace"))
                .build()
        }
        val store = eventStore(kernel, connectedLifecycle())

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking { store.eventLog.append("source", ProvisioningLifecycleEvent()) }
        }

        assertTrue(exception.message.orEmpty().contains("invalid namespace"))
        assertEquals(0, kernel.times("EventTypes.Register"))
        assertEquals(0, kernel.operations.count { it.startsWith("Append:") })
    }

    @Test
    fun `registration failure is retained and blocks every append in the generation`() {
        val kernel = startKernel().apply { failRegistration = true }
        val store = eventStore(kernel, connectedLifecycle())

        repeat(2) {
            assertThrows(Exception::class.java) {
                runBlocking { store.eventLog.append("source-$it", ProvisioningLifecycleEvent()) }
            }
        }

        assertEquals(1, kernel.times("EventTypes.Register"))
        assertEquals(0, kernel.operations.count { it.startsWith("Append:") })
    }

    @Test
    fun `reconnect retries a failed registration in a new generation`() {
        val kernel = startKernel().apply { failRegistration = true }
        val lifecycle = connectedLifecycle()
        val store = eventStore(kernel, lifecycle)

        assertThrows(Exception::class.java) {
            runBlocking { store.eventLog.append("before-reconnect", ProvisioningLifecycleEvent()) }
        }

        kernel.failRegistration = false
        lifecycle.markDisconnected()
        lifecycle.markConnected("connection-2")
        runBlocking { store.eventLog.append("after-reconnect", ProvisioningLifecycleEvent()) }

        assertEquals(2, kernel.times("EnsureEventStore"))
        assertEquals(2, kernel.times("EnsureNamespace:tenant-a"))
        assertEquals(2, kernel.times("EventTypes.Register"))
        assertEquals(1, kernel.times("Append:tenant-a"))
    }

    @Test
    fun `two namespaces sharing an event store provision and append independently`() = runTest {
        val kernel = startKernel()
        val lifecycle = connectedLifecycle()
        val first = eventStore(kernel, lifecycle, "tenant-a")
        val second = eventStore(kernel, lifecycle, "tenant-b")

        coroutineScope {
            awaitAll(
                async { first.eventLog.append("first", ProvisioningLifecycleEvent("first")) },
                async { second.eventLog.append("second", ProvisioningLifecycleEvent("second")) }
            )
        }

        assertEquals(2, kernel.times("EnsureEventStore"))
        assertEquals(1, kernel.times("EnsureNamespace:tenant-a"))
        assertEquals(1, kernel.times("EnsureNamespace:tenant-b"))
        assertEquals(1, kernel.times("Append:tenant-a"))
        assertEquals(1, kernel.times("Append:tenant-b"))
    }

    @Test
    fun `Java blocking first append follows the same provisioning order`() {
        val kernel = startKernel()
        val store = eventStore(kernel, connectedLifecycle())

        BlockingEventStore(store).eventLog.append("source", ProvisioningLifecycleEvent())

        assertEquals(
            listOf("EnsureEventStore", "EnsureNamespace:tenant-a", "EventTypes.Register", "Append:tenant-a"),
            kernel.operations
        )
    }

    private fun connectedLifecycle() = ConnectionLifecycle().apply { markConnected("connection-1") }

    private fun eventStore(
        kernel: KernelDouble,
        lifecycle: ConnectionLifecycle,
        namespace: String = "tenant-a"
    ) = EventStore(
        "my-store",
        namespace,
        ChronicleServices(checkNotNull(channel)),
        lifecycle,
        artifacts = KnownClientArtifacts(ProvisioningLifecycleEvent::class),
        autoDiscoverAndRegister = true
    )

    private fun startKernel(): KernelDouble {
        val kernel = KernelDouble()
        val runningServer = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(kernel.eventStores)
            .addService(kernel.namespaces)
            .addService(kernel.eventTypes)
            .addService(kernel.eventSequences)
            .build()
            .start()
        server = runningServer
        channel = Grpc.newChannelBuilderForAddress(
            "localhost",
            runningServer.port,
            InsecureChannelCredentials.create()
        ).build()
        return kernel
    }

    private class KernelDouble {
        val operations: MutableList<String> = Collections.synchronizedList(mutableListOf())
        var eventStoreResult: Eventstores.CommandResult = Eventstores.CommandResult.getDefaultInstance()
        var namespaceResult: NamespacesOuterClass.CommandResult = NamespacesOuterClass.CommandResult.getDefaultInstance()
        var registrationDelayMillis: Long = 0
        var failRegistration: Boolean = false

        fun times(operation: String): Int = operations.count { it == operation }

        val eventStores = object : EventStoresGrpcKt.EventStoresCoroutineImplBase() {
            override suspend fun ensureEventStore(
                request: Eventstores.EnsureEventStoreRequest
            ): Eventstores.CommandResult {
                operations += "EnsureEventStore"
                return eventStoreResult
            }
        }

        val namespaces = object : NamespacesGrpcKt.NamespacesCoroutineImplBase() {
            override suspend fun ensureNamespace(
                request: NamespacesOuterClass.EnsureNamespaceRequest
            ): NamespacesOuterClass.CommandResult {
                operations += "EnsureNamespace:${request.namespace}"
                return namespaceResult
            }
        }

        val eventTypes = object : EventTypesGrpcKt.EventTypesCoroutineImplBase() {
            override suspend fun register(request: Events.RegisterEventTypesRequest): Empty {
                operations += "EventTypes.Register"
                if (registrationDelayMillis > 0) delay(registrationDelayMillis)
                if (failRegistration) {
                    throw Status.INTERNAL.withDescription("registration failed").asRuntimeException()
                }
                return Empty.getDefaultInstance()
            }
        }

        val eventSequences = object : EventSequencesGrpcKt.EventSequencesCoroutineImplBase() {
            override suspend fun append(request: Eventsequences.AppendRequest): Eventsequences.AppendResponse {
                operations += "Append:${request.namespace}"
                return Eventsequences.AppendResponse.newBuilder().setSequenceNumber(1).build()
            }
        }
    }
}
