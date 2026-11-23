// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Multiplexed.ObservationMultiplexed
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The capability the kernel advertises when it serves every reactor and reducer of a connection over one stream. */
internal const val MULTIPLEXED_OBSERVATION_CAPABILITY = "multiplexed-observation"

/** The kernel sent a sub-stream message out of order, so nothing more on the shared call can be trusted. */
internal class MultiplexedSequenceGap(streamId: Long, expected: Long, actual: Long) :
    IllegalStateException("Multiplexed stream $streamId delivered sequence $actual where $expected was expected")

/**
 * One shared bidirectional call to the kernel that carries every reactor and reducer observation of a connection, each
 * as a sub-stream with its own id.
 *
 * It is a drop-in for the per-observer `observe` streams: [stream] takes the messages an observer sends and returns the
 * messages the kernel sends it, and fails with the same gRPC status a failed per-observer call would. The shared call
 * is opened by the first sub-stream and closed after the last one is gone; a sub-stream that starts once it has failed
 * gets a fresh call.
 *
 * @param openCall Opens the shared call: the messages to send up, the messages the kernel sends down.
 * @param coalesce How long to wait for companions before writing, so observers that start together register in one write.
 * @param maxMessagesPerWrite The most sub-stream messages carried by one write.
 * @param closeGrace How long a call with no sub-stream left is given to flush its `Close` messages before it is cancelled.
 */
internal class MultiplexedObservationChannel(
    private val openCall: (Flow<ObservationMultiplexed.MultiplexedClientMessage>) -> Flow<ObservationMultiplexed.MultiplexedServerMessage>,
    private val coalesce: Duration = 10.milliseconds,
    private val maxMessagesPerWrite: Int = 500,
    private val closeGrace: Duration = 5.seconds
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val nextStreamId = AtomicLong(1)
    private var current: SharedCall? = null

    /**
     * Observes over a sub-stream of the shared call.
     *
     * @param kind Whether the sub-stream is a reactor or a reducer.
     * @param requests What the observer sends: its registration first, then its results.
     * @param toUp Puts one of [requests] on a sub-stream message.
     * @param fromDown Picks the observer's message out of one the kernel sent, or `null` when it carries none.
     */
    fun <Up, Down : Any> stream(
        kind: ObservationMultiplexed.MultiplexedObserverKind,
        requests: Flow<Up>,
        toUp: (Up, ObservationMultiplexed.MultiplexedStreamMessage.Builder) -> Unit,
        fromDown: (ObservationMultiplexed.MultiplexedServerMessage) -> Down?
    ): Flow<Down> = flow {
        val (call, stream) = attach()
        try {
            coroutineScope {
                val sender = launch {
                    var first = true
                    requests.collect { request ->
                        val message = ObservationMultiplexed.MultiplexedStreamMessage.newBuilder().setStreamId(stream.id)
                        if (first) {
                            message.setKind(kind)
                            first = false
                        }
                        toUp(request, message)
                        call.send(message.build())
                    }
                    // The observer is done sending, which is how it says it is done observing.
                    call.close(stream)
                }
                try {
                    for (received in stream.inbox) {
                        fromDown(received)?.let { emit(it) }
                    }
                } finally {
                    sender.cancel()
                }
            }
        } finally {
            call.close(stream)
            release(call, stream)
        }
    }

    /** Stops the shared call, if there is one. Every open sub-stream ends. */
    fun shutdown() {
        synchronized(lock) { current?.also { current = null } }?.fail(CancellationException("The channel was shut down"))
    }

    private fun attach(): Pair<SharedCall, SubStream> = synchronized(lock) {
        val call = current?.takeIf { it.isUsable } ?: SharedCall().also { current = it; it.start() }
        val stream = SubStream(nextStreamId.getAndIncrement())
        call.streams[stream.id] = stream
        call to stream
    }

    private fun release(call: SharedCall, stream: SubStream) {
        val last = synchronized(lock) {
            call.streams.remove(stream.id)
            if (call.streams.isEmpty() && current === call) {
                current = null
                true
            } else {
                false
            }
        }
        if (last) call.retire()
    }

    /** What a sub-stream receives: the kernel's messages, then the end. */
    private class SubStream(val id: Long) {
        val inbox = Channel<ObservationMultiplexed.MultiplexedServerMessage>(Channel.UNLIMITED)
        var expectedSequence = 1L
        val closeSent = AtomicBoolean(false)
    }

    private inner class SharedCall {
        val streams = ConcurrentHashMap<Long, SubStream>()
        private val outgoing = Channel<ObservationMultiplexed.MultiplexedStreamMessage>(Channel.UNLIMITED)
        private var job: Job? = null

        @Volatile
        private var ended = false

        @Volatile
        private var retired = false

        val isUsable: Boolean get() = !ended && !retired

        fun start() {
            job = scope.launch {
                try {
                    openCall(writes()).collect { route(it) }
                    endStreams(null)
                } catch (error: CancellationException) {
                    endStreams(null)
                    throw error
                } catch (error: Throwable) {
                    endStreams(error)
                }
            }
        }

        fun send(message: ObservationMultiplexed.MultiplexedStreamMessage) {
            outgoing.trySend(message)
        }

        /** Tells the kernel the sub-stream is finished, once. */
        fun close(stream: SubStream) {
            if (!stream.closeSent.compareAndSet(false, true)) return
            send(ObservationMultiplexed.MultiplexedStreamMessage.newBuilder().setStreamId(stream.id).setClose(true).build())
        }

        /** No sub-stream is left: let the closes go out, then end the call. */
        fun retire() {
            retired = true
            outgoing.close()
            scope.launch {
                delay(closeGrace)
                job?.cancel()
            }
        }

        fun fail(error: Throwable) {
            retired = true
            outgoing.close()
            job?.cancel()
            endStreams(error)
        }

        private fun endStreams(error: Throwable?) {
            ended = true
            outgoing.close()
            for (stream in streams.values) {
                stream.inbox.close(error)
            }
        }

        private fun writes(): Flow<ObservationMultiplexed.MultiplexedClientMessage> = flow {
            while (true) {
                val batch = ObservationMultiplexed.MultiplexedClientMessage.newBuilder()
                val first = outgoing.receiveCatching().getOrNull() ?: return@flow
                batch.addMessages(first)
                // Whoever registers within the window rides in the same write. Waiting, then taking what is there,
                // never abandons a message the way cancelling a receive could.
                delay(coalesce)
                var count = 1
                while (count < maxMessagesPerWrite) {
                    val next = outgoing.tryReceive().getOrNull() ?: break
                    batch.addMessages(next)
                    count++
                }
                emit(batch.build())
            }
        }

        private fun route(message: ObservationMultiplexed.MultiplexedServerMessage) {
            // A message for a sub-stream that is already gone is the kernel catching up with a close.
            val stream = streams[message.streamId] ?: return
            if (message.sequence != stream.expectedSequence) {
                throw MultiplexedSequenceGap(stream.id, stream.expectedSequence, message.sequence)
            }
            stream.expectedSequence++

            if (message.hasEnd()) {
                val end = message.end
                if (end.statusCode == 0) {
                    stream.inbox.close()
                } else {
                    stream.inbox.close(Status.fromCodeValue(end.statusCode).withDescription(end.detail).asRuntimeException())
                }
                return
            }
            stream.inbox.trySend(message)
        }
    }
}
