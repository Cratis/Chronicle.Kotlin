// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Multiplexed.ObservationMultiplexed
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

/** The shared observation call: demultiplexing, ordering, closing, failing, and a slow observer. */
class MultiplexedObservationChannelTests {

    /** A kernel the channel talks to: records what is written, and says what it is told to. */
    private class FakeKernel {
        val calls = CopyOnWriteArrayList<FakeCall>()

        fun open(up: Flow<ObservationMultiplexed.MultiplexedClientMessage>): Flow<ObservationMultiplexed.MultiplexedServerMessage> = flow {
            val call = FakeCall()
            calls += call
            coroutineScope {
                val reader = launch { up.collect { call.writes += it } }
                try {
                    for (message in call.down) emit(message)
                } finally {
                    reader.cancel()
                }
            }
        }
    }

    private class FakeCall {
        val writes = CopyOnWriteArrayList<ObservationMultiplexed.MultiplexedClientMessage>()
        val down = Channel<ObservationMultiplexed.MultiplexedServerMessage>(Channel.UNLIMITED)
        private val sequences = HashMap<Long, Long>()

        val messages: List<ObservationMultiplexed.MultiplexedStreamMessage> get() = writes.flatMap { it.messagesList }

        fun deliver(streamId: Long, partition: String) {
            val sequence = sequences.merge(streamId, 1L, Long::plus)!!
            down.trySend(
                ObservationMultiplexed.MultiplexedServerMessage.newBuilder().setStreamId(streamId).setSequence(sequence)
                    .setReactor(ObservationMultiplexed.EventsToObserve.newBuilder().setPartition(partition)).build()
            )
        }

        fun deliverWithSequence(streamId: Long, sequence: Long, partition: String) {
            down.trySend(
                ObservationMultiplexed.MultiplexedServerMessage.newBuilder().setStreamId(streamId).setSequence(sequence)
                    .setReactor(ObservationMultiplexed.EventsToObserve.newBuilder().setPartition(partition)).build()
            )
        }

        fun end(streamId: Long, status: Int = 0, detail: String = "") {
            val sequence = sequences.merge(streamId, 1L, Long::plus)!!
            down.trySend(
                ObservationMultiplexed.MultiplexedServerMessage.newBuilder().setStreamId(streamId).setSequence(sequence)
                    .setEnd(ObservationMultiplexed.MultiplexedStreamEnd.newBuilder().setStatusCode(status).setDetail(detail)).build()
            )
        }
    }

    private val kernel = FakeKernel()
    private val channel = MultiplexedObservationChannel(kernel::open, coalesce = 5.milliseconds, closeGrace = 50.milliseconds)

    /** An observer: sends its registration, then whatever is put in [results]. */
    private inner class Observer(private val kind: ObservationMultiplexed.MultiplexedObserverKind = ObservationMultiplexed.MultiplexedObserverKind.Reactor) {
        val results = Channel<ObservationMultiplexed.ReactorMessage>(Channel.UNLIMITED)
        val received = CopyOnWriteArrayList<String>()
        val finished = CompletableDeferred<Throwable?>()

        fun stream(): Flow<String> = channel.stream(
            kind,
            results.receiveAsFlow(),
            { request, message -> message.setReactor(request) },
            { if (it.hasReactor()) it.reactor.partition else null }
        )

        fun register() {
            results.trySend(ObservationMultiplexed.ReactorMessage.getDefaultInstance())
        }
    }

    private fun CoroutineScope.collecting(observer: Observer): Deferred<Throwable?> = async {
        try {
            observer.stream().collect { observer.received += it }
            null
        } catch (error: Throwable) {
            error
        }
    }

    private suspend fun until(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMillis) { while (!condition()) delay(2) }
    }

    @Test
    fun `observers that start together register in one write`() = runBlocking {
        val observers = List(3) { Observer().also { it.register() } }
        val collectors = observers.map { collecting(it) }

        until { kernel.calls.singleOrNull()?.messages?.size == 3 }

        assertEquals(1, kernel.calls.size)
        assertEquals(1, kernel.calls.single().writes.size)
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `each observer gets its own stream id and only the first message carries the kind`() = runBlocking {
        val first = Observer().also { it.register() }
        val second = Observer(ObservationMultiplexed.MultiplexedObserverKind.Reducer).also { it.register() }
        val collectors = listOf(collecting(first), collecting(second))
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        first.register()
        until { kernel.calls.single().messages.size == 3 }

        val messages = kernel.calls.single().messages
        assertEquals(2, messages.map { it.streamId }.distinct().size)
        val ofFirst = messages.filter { it.streamId == messages.first().streamId }
        assertEquals(ObservationMultiplexed.MultiplexedObserverKind.Reactor, ofFirst.first().kind)
        assertEquals(ObservationMultiplexed.MultiplexedObserverKind.Unspecified, ofFirst.last().kind)
        assertEquals(ObservationMultiplexed.MultiplexedObserverKind.Reducer, messages.first { it.streamId != ofFirst.first().streamId }.kind)
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `messages are delivered to the observer of their stream, in order`() = runBlocking {
        val first = Observer().also { it.register() }
        val second = Observer().also { it.register() }
        val collectors = listOf(collecting(first), collecting(second))
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        val (firstId, secondId) = kernel.calls.single().messages.map { it.streamId }

        val call = kernel.calls.single()
        call.deliver(firstId, "a1")
        call.deliver(secondId, "b1")
        call.deliver(firstId, "a2")
        until { first.received.size == 2 && second.received.size == 1 }

        assertEquals(listOf("a1", "a2"), first.received)
        assertEquals(listOf("b1"), second.received)
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `a sequence gap fails the whole call`() = runBlocking {
        val first = Observer().also { it.register() }
        val second = Observer().also { it.register() }
        val collectors = listOf(collecting(first), collecting(second))
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        val firstId = kernel.calls.single().messages.first().streamId

        kernel.calls.single().deliverWithSequence(firstId, 2, "skipped one")

        collectors.forEach { assertInstanceOf(MultiplexedSequenceGap::class.java, withTimeout(5_000) { it.await() }) }
    }

    @Test
    fun `a stream the kernel ends with an error fails only that observer, with the status it ended with`() = runBlocking {
        val failing = Observer().also { it.register() }
        val healthy = Observer().also { it.register() }
        val failingCollector = collecting(failing)
        val healthyCollector = collecting(healthy)
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        val (failingId, healthyId) = kernel.calls.single().messages.map { it.streamId }

        kernel.calls.single().end(failingId, Status.Code.NOT_FOUND.value(), "no such observer")
        val error = withTimeout(5_000) { failingCollector.await() } as StatusRuntimeException
        kernel.calls.single().deliver(healthyId, "still here")
        until { healthy.received.isNotEmpty() }

        assertEquals(Status.Code.NOT_FOUND, error.status.code)
        assertEquals("no such observer", error.status.description)
        assertFalse(healthyCollector.isCompleted)
        healthyCollector.cancel()
    }

    @Test
    fun `a stream the kernel ends cleanly completes only that observer`() = runBlocking {
        val ending = Observer().also { it.register() }
        val staying = Observer().also { it.register() }
        val endingCollector = collecting(ending)
        val stayingCollector = collecting(staying)
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }

        kernel.calls.single().end(kernel.calls.single().messages.first().streamId)

        assertEquals(null, withTimeout(5_000) { endingCollector.await() })
        assertFalse(stayingCollector.isCompleted)
        stayingCollector.cancel()
    }

    @Test
    fun `an observer that stops tells the kernel to close its stream, and the last one to stop ends the call`() = runBlocking {
        val observer = Observer().also { it.register() }
        val collector = collecting(observer)
        until { kernel.calls.singleOrNull()?.messages?.size == 1 }
        val streamId = kernel.calls.single().messages.single().streamId

        collector.cancel()

        until { kernel.calls.single().messages.any { it.close } }
        assertEquals(streamId, kernel.calls.single().messages.single { it.close }.streamId)
        // The call is over, so the next observer starts a call of its own.
        val next = Observer().also { it.register() }
        val nextCollector = collecting(next)
        until { kernel.calls.size == 2 }
        nextCollector.cancel()
    }

    @Test
    fun `the call is kept while an observer is still on it`() = runBlocking {
        val stopping = Observer().also { it.register() }
        val staying = Observer().also { it.register() }
        val stoppingCollector = collecting(stopping)
        val stayingCollector = collecting(staying)
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }

        stoppingCollector.cancel()
        until { kernel.calls.single().messages.any { it.close } }
        val third = Observer().also { it.register() }
        val thirdCollector = collecting(third)
        until { kernel.calls.single().messages.size == 4 }

        assertEquals(1, kernel.calls.size)
        stayingCollector.cancel()
        thirdCollector.cancel()
    }

    @Test
    fun `when the shared call fails every observer fails and the next one gets a new call`() = runBlocking {
        val first = Observer().also { it.register() }
        val second = Observer().also { it.register() }
        val collectors = listOf(collecting(first), collecting(second))
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }

        kernel.calls.single().down.close(Status.UNAVAILABLE.withDescription("connection lost").asRuntimeException())

        collectors.forEach {
            val error = withTimeout(5_000) { it.await() } as StatusRuntimeException
            assertEquals(Status.Code.UNAVAILABLE, error.status.code)
        }
        val again = Observer().also { it.register() }
        val againCollector = collecting(again)
        until { kernel.calls.size == 2 }
        againCollector.cancel()
    }

    @Test
    fun `when the shared call ends every observer completes`() = runBlocking {
        val observer = Observer().also { it.register() }
        val collector = collecting(observer)
        until { kernel.calls.singleOrNull()?.messages?.size == 1 }

        kernel.calls.single().down.close()

        assertEquals(null, withTimeout(5_000) { collector.await() })
    }

    @Test
    fun `a slow observer does not hold up another`() = runBlocking {
        val slow = Observer().also { it.register() }
        val fast = Observer().also { it.register() }
        val release = CompletableDeferred<Unit>()
        val slowCollector = async {
            slow.stream().collect {
                slow.received += it
                release.await()
            }
        }
        val fastCollector = collecting(fast)
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        val (slowId, fastId) = kernel.calls.single().messages.map { it.streamId }

        val call = kernel.calls.single()
        repeat(50) { call.deliver(slowId, "slow-$it") }
        repeat(50) { call.deliver(fastId, "fast-$it") }
        until { fast.received.size == 50 }

        assertEquals(50, fast.received.size)
        assertEquals(1, slow.received.size)
        release.complete(Unit)
        until { slow.received.size == 50 }
        assertTrue(slow.received.zipWithNext().all { (a, b) -> a.substringAfter('-').toInt() < b.substringAfter('-').toInt() })
        slowCollector.cancel()
        fastCollector.cancel()
    }

    @Test
    fun `a message for a stream that has gone is ignored`() = runBlocking {
        val observer = Observer().also { it.register() }
        val staying = Observer().also { it.register() }
        val collector = collecting(observer)
        val stayingCollector = collecting(staying)
        until { kernel.calls.singleOrNull()?.messages?.size == 2 }
        val (goneId, _) = kernel.calls.single().messages.map { it.streamId }
        collector.cancel()
        until { kernel.calls.single().messages.any { it.close } }

        kernel.calls.single().deliver(goneId, "late")
        delay(30)

        assertFalse(stayingCollector.isCompleted)
        stayingCollector.cancel()
    }
}
