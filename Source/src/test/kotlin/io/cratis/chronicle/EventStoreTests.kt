// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

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
    fun `awaitRegistration returns straight away when automatic registration is turned off`() = runTest {
        // No kernel is reachable here, so a call that waited would never come back.
        withTimeout(1000) { newEventStore().awaitRegistration() }
    }
}
