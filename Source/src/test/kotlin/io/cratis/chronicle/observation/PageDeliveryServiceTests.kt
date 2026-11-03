// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.IEventLog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The client asks the kernel for pages when it registers an observer, and answers a page it is sent
 * with one result per partition.
 */
class PageDeliveryServiceTests {
    @Reactor("paged-reactor")
    class PagedReactor {
        val seen = mutableListOf<Long>()

        @Observes(AccountOpened::class)
        fun opened(batch: EventBatch) {
            seen += batch.map { it.context.sequenceNumber }
        }
    }

    @Reducer("paged-reducer")
    class PagedReducer {
        @Observes(AccountOpened::class)
        fun reduce(events: EventBatch, @Suppress("UNUSED_PARAMETER") current: AccountBalance?): AccountBalance =
            AccountBalance((events.last().event as AccountOpened).owner)
    }

    private fun reactorEvent(sequenceNumber: Long, eventSourceId: String) = ObservationReactors.AppendedEvent.newBuilder()
        .setContext(
            ObservationReactors.EventContext.newBuilder()
                .setSequenceNumber(sequenceNumber)
                .setEventSourceId(eventSourceId)
                .setEventType(ObservationReactors.EventType.newBuilder().setId("AccountOpened").setGeneration(1))
        )
        .setContent("{\"owner\":\"$eventSourceId\"}")
        .build()

    private fun reducerEvent(sequenceNumber: Long, eventSourceId: String) = ObservationReducers.AppendedEvent.newBuilder()
        .setContext(
            ObservationReducers.EventContext.newBuilder()
                .setSequenceNumber(sequenceNumber)
                .setEventSourceId(eventSourceId)
                .setEventType(ObservationReducers.EventType.newBuilder().setId("AccountOpened").setGeneration(1))
        )
        .setContent("{\"owner\":\"$eventSourceId\"}")
        .build()

    @Test
    fun `a reactor asks for pages and answers a page with one result per partition`() = runBlocking {
        val lifecycle = ConnectionLifecycle()
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        val registered = CompletableDeferred<ObservationReactors.ReactorMessage>()
        val answered = CompletableDeferred<ObservationReactors.ReactorMessage>()
        val page = ObservationReactors.EventsToObserve.newBuilder()
            .setDeliveryId(5)
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("a").addEvents(reactorEvent(1, "a")))
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("b").addEvents(reactorEvent(2, "b")))
            .build()

        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReactors.ReactorMessage>>()
            flow {
                launch {
                    requests.collect { if (!registered.isCompleted) registered.complete(it) else answered.complete(it) }
                }
                emit(page)
                awaitCancellation()
            }
        }
        lifecycle.markConnected(lifecycle.connectionId)
        val reactor = PagedReactor()
        val job = ReactorsService("store", "default", lifecycle, stub, mockk<IEventLog>()).register(reactor)
        try {
            val registration = withTimeout(5_000) { registered.await() }.content.value0
            assertTrue(registration.pageDelivery.enabled)

            val result = withTimeout(5_000) { answered.await() }.content.value1
            assertEquals(5L, result.deliveryId)
            assertEquals(listOf("a", "b"), result.partitionsList.map { it.partition })
            assertTrue(result.partitionsList.all { it.state == ObservationReactors.ObservationState.Success })
            assertEquals(listOf(1L, 2L), reactor.seen)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `a reducer asks for pages and answers a page with each event source's state`() = runBlocking {
        val lifecycle = ConnectionLifecycle()
        val stub = mockk<ReducersGrpcKt.ReducersCoroutineStub>()
        val registered = CompletableDeferred<ObservationReducers.ReducerMessage>()
        val answered = CompletableDeferred<ObservationReducers.ReducerMessage>()
        val page = ObservationReducers.ReduceOperationMessage.newBuilder()
            .setDeliveryId(6)
            .addPartitions(ObservationReducers.ReducerPartition.newBuilder().setPartition("a").addEvents(reducerEvent(1, "a")))
            .addPartitions(ObservationReducers.ReducerPartition.newBuilder().setPartition("b").addEvents(reducerEvent(2, "b")))
            .build()

        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReducers.ReducerMessage>>()
            flow {
                launch {
                    requests.collect { if (!registered.isCompleted) registered.complete(it) else answered.complete(it) }
                }
                emit(page)
                awaitCancellation()
            }
        }
        lifecycle.markConnected(lifecycle.connectionId)
        val job = ReducersService("store", "default", lifecycle, stub).register(PagedReducer())
        try {
            val registration = withTimeout(5_000) { registered.await() }.content.value0
            assertTrue(registration.pageDelivery.enabled)

            val result = withTimeout(5_000) { answered.await() }.content.value1
            assertEquals(6L, result.deliveryId)
            assertEquals(listOf("a", "b"), result.partitionsList.map { it.partition })
            assertEquals(listOf("{\"owner\":\"a\",\"balance\":0}", "{\"owner\":\"b\",\"balance\":0}"), result.partitionsList.map { it.readModelState })
        } finally {
            job.cancel()
        }
    }
}
