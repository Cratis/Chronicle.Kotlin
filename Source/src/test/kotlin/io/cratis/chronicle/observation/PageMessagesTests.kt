// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * A kernel that delivers pages sends `Partitions` and is answered with one result per partition. One
 * that does not sends a single partition in the message's own fields, and is answered in kind.
 */
class PageMessagesTests {
    private fun reactorEvent(sequenceNumber: Long, eventSourceId: String) = ObservationReactors.AppendedEvent.newBuilder()
        .setContext(
            ObservationReactors.EventContext.newBuilder()
                .setSequenceNumber(sequenceNumber)
                .setEventSourceId(eventSourceId)
                .setEventType(ObservationReactors.EventType.newBuilder().setId("AccountOpened").setGeneration(1))
        )
        .setContent("{\"owner\":\"ann\"}")
        .build()

    private fun reducerEvent(sequenceNumber: Long, eventSourceId: String) = ObservationReducers.AppendedEvent.newBuilder()
        .setContext(
            ObservationReducers.EventContext.newBuilder()
                .setSequenceNumber(sequenceNumber)
                .setEventSourceId(eventSourceId)
                .setEventType(ObservationReducers.EventType.newBuilder().setId("AccountOpened").setGeneration(1))
        )
        .setContent("{\"owner\":\"ann\"}")
        .build()

    @Test
    fun `a reactor page is read partition by partition, with its origin`() {
        val message = ObservationReactors.EventsToObserve.newBuilder()
            .setDeliveryId(42)
            .setOrigin(ObservationReactors.ObservationOrigin.Replaying)
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("a").addEvents(reactorEvent(1, "a")))
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("b").addEvents(reactorEvent(2, "b")))
            .build()

        val page = message.toDeliveredPage()

        assertTrue(page.isPage)
        assertEquals(ObservationOrigin.Replay, page.origin)
        assertEquals(listOf("a", "b"), page.partitions.map { it.partition })
        assertEquals(listOf(listOf(1L), listOf(2L)), page.partitions.map { it.sequenceNumbers })
        assertTrue(page.contextFor("o", "s", "n", "e").isReplay)
    }

    @Test
    fun `a reactor page is answered with one result per partition, including deferrals`() {
        val message = ObservationReactors.EventsToObserve.newBuilder()
            .setDeliveryId(42)
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("a").addEvents(reactorEvent(1, "a")))
            .build()

        val result = message.resultFor(
            listOf(
                PartitionOutcome("a", PartitionState.Success, 1),
                PartitionOutcome("b", PartitionState.Deferred, PartitionOutcome.NONE_OBSERVED, retryAfter = Duration.ofSeconds(3)),
            ),
        )

        assertEquals(42L, result.deliveryId)
        assertEquals(listOf("a", "b"), result.partitionsList.map { it.partition })
        assertEquals(ObservationReactors.ObservationState.Deferred, result.partitionsList[1].state)
        assertEquals(3_000L, result.partitionsList[1].retryAfterMilliseconds)
        assertEquals(-1L, result.partitionsList[1].lastSuccessfulObservation)
    }

    @Test
    fun `a single-partition reactor delivery is read as a page of one that cannot defer`() {
        val message = ObservationReactors.EventsToObserve.newBuilder()
            .setPartition("a")
            .addEvents(reactorEvent(1, "a"))
            .addEvents(reactorEvent(2, "a"))
            .build()

        val page = message.toDeliveredPage()

        assertFalse(page.isPage)
        assertEquals(listOf("a"), page.partitions.map { it.partition })
        assertEquals(listOf(1L, 2L), page.partitions.single().sequenceNumbers)
        assertFalse(page.contextFor("o", "s", "n", "e").canDefer)
    }

    @Test
    fun `a single-partition reactor delivery is answered in its own fields`() {
        val message = ObservationReactors.EventsToObserve.newBuilder().setPartition("a").setDeliveryId(7).addEvents(reactorEvent(1, "a")).build()

        val result = message.resultFor(listOf(PartitionOutcome("a", PartitionState.Failed, 0, listOf("boom"), "trace")))

        assertEquals(0, result.partitionsCount)
        assertEquals("a", result.partition)
        assertEquals(7L, result.deliveryId)
        assertEquals(ObservationReactors.ObservationState.Failed, result.state)
        assertEquals(listOf("boom"), result.exceptionMessagesList)
    }

    @Test
    fun `a reducer page carries each event source's stored state`() {
        val message = ObservationReducers.ReduceOperationMessage.newBuilder()
            .setOrigin(ObservationReducers.ObservationOrigin.Live)
            .addPartitions(ObservationReducers.ReducerPartition.newBuilder().setPartition("a").setInitialState("{}").addEvents(reducerEvent(1, "a")))
            .build()

        val page = message.toDeliveredPage()

        assertTrue(page.isPage)
        assertEquals(ObservationOrigin.Live, page.origin)
        assertEquals("{}", page.partitions.single().initialState)
    }

    @Test
    fun `a single-partition reducer delivery is answered in its own fields, with the state`() {
        val message = ObservationReducers.ReduceOperationMessage.newBuilder()
            .setPartition("a")
            .setInitialState("{}")
            .addEvents(reducerEvent(1, "a"))
            .build()

        assertEquals("{}", message.toDeliveredPage().partitions.single().initialState)

        val result = message.resultFor(listOf(PartitionOutcome("a", PartitionState.Success, 1, readModelState = "{\"owner\":\"ann\"}")))

        assertEquals("a", result.partition)
        assertEquals("{\"owner\":\"ann\"}", result.readModelState)
        assertEquals(ObservationReducers.ObservationState.Success, result.state)
    }

    @Test
    fun `a reducer page is answered with one result per partition`() {
        val message = ObservationReducers.ReduceOperationMessage.newBuilder()
            .setDeliveryId(9)
            .addPartitions(ObservationReducers.ReducerPartition.newBuilder().setPartition("a").addEvents(reducerEvent(1, "a")))
            .build()

        val result = message.resultFor(listOf(PartitionOutcome("a", PartitionState.Success, 1, readModelState = "{}")))

        assertEquals(9L, result.deliveryId)
        assertEquals("{}", result.partitionsList.single().readModelState)
    }
}
