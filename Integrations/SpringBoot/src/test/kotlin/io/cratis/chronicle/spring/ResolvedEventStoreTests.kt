// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.IChronicleClient
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResolvedEventStoreTests {

    // A channel is safe to construct without a live server - gRPC only dials on first RPC, and nothing
    // here issues one.
    private val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()

    private fun storeFor(namespace: String) =
        EventStore("Ordering", namespace, ChronicleServices(channel), ConnectionLifecycle())

    private val client = mockk<IChronicleClient>().also {
        every { it.getEventStore("Ordering", any()) } answers { storeFor(secondArg()) }
    }

    @Test
    fun `routes to the namespace the current work belongs to`() {
        var namespace = "acme"
        val subject = ResolvedEventStore(client, "Ordering", IEventStoreNamespaceResolver { namespace })

        assertThat(subject.namespace).isEqualTo("acme")

        namespace = "globex"

        assertThat(subject.namespace).isEqualTo("globex")
    }

    @Test
    fun `works against the event store it was asked for`() {
        val subject = ResolvedEventStore(client, "Ordering", IEventStoreNamespaceResolver { "acme" })

        assertThat(subject.name).isEqualTo("Ordering")
    }

    @Test
    fun `hands out the services of the store it resolved`() {
        val subject = ResolvedEventStore(client, "Ordering", IEventStoreNamespaceResolver { "acme" })

        assertThat(subject.eventLog).isNotNull()
        assertThat(subject.readModels).isNotNull()
    }
}
