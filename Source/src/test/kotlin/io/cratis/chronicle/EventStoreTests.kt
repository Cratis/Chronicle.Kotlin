// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.connection.ChronicleConnectionFailed
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.java.BlockingEventStore
import io.cratis.chronicle.java.EventStoreJavaBridge
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.Status
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventStoreTests {

    // A channel is safe to construct without a live server - gRPC only dials on first RPC,
    // and none of the assertions below issue one.
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
    fun `a rejected connection fails a pending registration wait and the first append`() = runTest {
        val lifecycle = ConnectionLifecycle()
        val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()
        val store = EventStore("my-store", "default", ChronicleServices(channel), lifecycle, autoDiscoverAndRegister = true)
        val waiter = async { runCatching { store.awaitRegistration() } }
        val append = async { runCatching { store.eventLog.append("order-1", Any()) } }
        runCurrent()
        assertFalse(waiter.isCompleted)
        assertFalse(append.isCompleted)

        val rejection = Status.UNAUTHENTICATED.asRuntimeException()
        lifecycle.markTerminalFailure(rejection)
        runCurrent()

        assertSame(rejection, assertInstanceOf(ChronicleConnectionFailed::class.java, waiter.await().exceptionOrNull()).cause)
        assertSame(rejection, assertInstanceOf(ChronicleConnectionFailed::class.java, append.await().exceptionOrNull()).cause)
        val later = runCatching { store.awaitRegistration() }
        assertInstanceOf(ChronicleConnectionFailed::class.java, later.exceptionOrNull())
        channel.shutdownNow()
    }

    @Test
    fun `a transient disconnect keeps registration waiting`() = runTest {
        val lifecycle = ConnectionLifecycle()
        val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()
        val store = EventStore("my-store", "default", ChronicleServices(channel), lifecycle, autoDiscoverAndRegister = true)
        val waiter = async { runCatching { store.awaitRegistration() } }
        runCurrent()
        lifecycle.markDisconnected()
        runCurrent()
        assertFalse(waiter.isCompleted)

        lifecycle.markTerminalFailure(Status.PERMISSION_DENIED.asRuntimeException())
        runCurrent()
        assertInstanceOf(ChronicleConnectionFailed::class.java, waiter.await().exceptionOrNull())
        channel.shutdownNow()
    }

    @Test
    fun `Java blocking registration and append expose connection rejection`() {
        val lifecycle = ConnectionLifecycle()
        val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()
        val store = EventStore("my-store", "default", ChronicleServices(channel), lifecycle, autoDiscoverAndRegister = true)
        val rejection = Status.UNAUTHENTICATED.asRuntimeException()
        lifecycle.markTerminalFailure(rejection)
        try {
            assertSame(rejection, assertThrows(ChronicleConnectionFailed::class.java) {
                BlockingEventStore(store).awaitRegistration()
            }.cause)
            assertSame(rejection, assertThrows(ChronicleConnectionFailed::class.java) {
                EventStoreJavaBridge.awaitRegistration(store)
            }.cause)
            assertSame(rejection, assertThrows(ChronicleConnectionFailed::class.java) {
                BlockingEventStore(store).eventLog.append("order-1", Any())
            }.cause)
        } finally {
            channel.shutdownNow()
        }
    }

    @Test
    fun `awaitRegistration returns straight away when automatic registration is turned off`() = runTest {
        // No kernel is reachable here, so a call that waited would never come back.
        withTimeout(1000) { newEventStore().awaitRegistration() }
    }
}
