// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.IChronicleClient
import kotlinx.coroutines.runBlocking

/**
 * Chronicle for Java, without the coroutines.
 *
 * The client's own API is coroutine-first, which is right for Kotlin and unusable from Java: a
 * `suspend fun` carries a hidden continuation on the JVM, so calling one meant hand-assembling a
 * `BuildersKt.runBlocking` with a cast continuation for a single append. This is the same client
 * with the waiting done for you.
 *
 * ```java
 * var client = BlockingChronicleClient.connect(ChronicleOptions.development());
 * var eventStore = client.getEventStore("ChronicleConsole");
 *
 * eventStore.getEventLog().append("some-event-source", new TestEvent("Hello world!"));
 * ```
 *
 * Every call blocks until the kernel answers, which is what a `main`, a controller method or a
 * scheduled job wants. Do not call these from inside a coroutine - Kotlin has [IChronicleClient].
 *
 * Artifacts still register themselves: the append above waits for that to finish, so there is
 * nothing to declare and nothing to sequence.
 *
 * @param client The client to forward to.
 */
class BlockingChronicleClient(private val client: IChronicleClient) : AutoCloseable {

    /** The suspending client underneath. */
    fun unwrap(): IChronicleClient = client

    /**
     * The event store of this name, in the default namespace.
     *
     * @param name The name of the event store.
     */
    fun getEventStore(name: String): BlockingEventStore =
        getEventStore(name, EventStoreNamespaceName.default.value)

    /**
     * The event store of this name, in [namespace].
     *
     * @param name The name of the event store.
     * @param namespace The namespace within it.
     */
    fun getEventStore(name: String, namespace: String): BlockingEventStore =
        BlockingEventStore(client.getEventStore(name, namespace))

    /** Every event store the kernel knows about, not only the ones this client has opened. */
    fun getEventStores(): List<String> = runBlocking { client.getEventStores() }

    /** Releases every cached event store without disposing the client. */
    fun evictEventStores() = client.evictEventStores()

    /** Releases everything the client holds. */
    fun dispose() = client.dispose()

    override fun close() = dispose()

    companion object {
        /**
         * Connects with [options] and hands back a client Java can use.
         *
         * @param options How to reach the kernel, and what to discover.
         */
        @JvmStatic
        fun connect(options: ChronicleOptions): BlockingChronicleClient =
            BlockingChronicleClient(ChronicleClient(options))
    }
}
