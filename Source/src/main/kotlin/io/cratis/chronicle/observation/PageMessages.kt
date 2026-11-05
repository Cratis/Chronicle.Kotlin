// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import io.cratis.chronicle.eventSequences.AppendedEvent

/**
 * Conversions between the delivery and result messages on the wire and [DeliveredPage] and
 * [PartitionOutcome].
 *
 * A message that carries `Partitions` is a page. One that does not is the single-partition shape an
 * older kernel sends - or any kernel, to a client that did not ask for pages - and its own
 * `Partition` and `Events` fields are the page's only partition.
 */

/** The delivery as a [DeliveredPage]. */
internal fun ObservationReactors.EventsToObserve.toDeliveredPage(): DeliveredPage =
    if (partitionsCount > 0) {
        DeliveredPage(
            partitionsList.map { DeliveredPartition(it.partition, it.eventsList.map { e -> e.toAppendedEvent() }) },
            origin.toClient(),
            deliverWithinMilliseconds,
            isPage = true,
        )
    } else {
        DeliveredPage(
            listOf(DeliveredPartition(partition, eventsList.map { it.toAppendedEvent() })),
            origin.toClient(),
            deliverWithinMilliseconds,
            isPage = false,
        )
    }

/** The delivery as a [DeliveredPage]. */
internal fun ObservationReducers.ReduceOperationMessage.toDeliveredPage(): DeliveredPage =
    if (partitionsCount > 0) {
        DeliveredPage(
            partitionsList.map {
                DeliveredPartition(it.partition, it.eventsList.map { e -> e.toAppendedEvent() }, it.initialState)
            },
            origin.toClient(),
            deliverWithinMilliseconds,
            isPage = true,
        )
    } else {
        DeliveredPage(
            listOf(DeliveredPartition(partition, eventsList.map { it.toAppendedEvent() }, initialState)),
            origin.toClient(),
            deliverWithinMilliseconds,
            isPage = false,
        )
    }

/** The reactor's answer for [outcomes] of a delivery made with this message. */
internal fun ObservationReactors.EventsToObserve.resultFor(outcomes: List<PartitionOutcome>): ObservationReactors.ReactorResult {
    val result = ObservationReactors.ReactorResult.newBuilder().setDeliveryId(deliveryId)
    if (partitionsCount > 0) {
        outcomes.forEach { result.addPartitions(it.toReactorContract()) }
    } else {
        val outcome = outcomes.single()
        result.setPartition(partition)
            .setState(outcome.state.forSinglePartition().toReactorContract())
            .setLastSuccessfulObservation(outcome.lastSuccessfulObservation)
            .addAllExceptionMessages(outcome.messages)
            .setExceptionStackTrace(outcome.stackTrace)
    }
    return result.build()
}

/** The reducer's answer for [outcomes] of a delivery made with this message. */
internal fun ObservationReducers.ReduceOperationMessage.resultFor(outcomes: List<PartitionOutcome>): ObservationReducers.ReducerResult {
    val result = ObservationReducers.ReducerResult.newBuilder().setDeliveryId(deliveryId)
    if (partitionsCount > 0) {
        outcomes.forEach { result.addPartitions(it.toReducerContract()) }
    } else {
        val outcome = outcomes.single()
        result.setPartition(partition)
            .setState(outcome.state.forSinglePartition().toReducerContract())
            .setLastSuccessfulObservation(outcome.lastSuccessfulObservation)
            .addAllExceptionMessages(outcome.messages)
            .setExceptionStackTrace(outcome.stackTrace)
        outcome.readModelState?.let { result.setReadModelState(it) }
    }
    return result.build()
}

private fun PartitionOutcome.toReactorContract(): ObservationReactors.PartitionResult =
    ObservationReactors.PartitionResult.newBuilder()
        .setPartition(partition)
        .setState(state.toReactorContract())
        .setLastSuccessfulObservation(lastSuccessfulObservation)
        .addAllExceptionMessages(messages)
        .setExceptionStackTrace(stackTrace)
        .setRetryAfterMilliseconds(retryAfter.toMillis().coerceAtLeast(0))
        .build()

private fun PartitionOutcome.toReducerContract(): ObservationReducers.PartitionResult {
    val result = ObservationReducers.PartitionResult.newBuilder()
        .setPartition(partition)
        .setState(state.toReducerContract())
        .setLastSuccessfulObservation(lastSuccessfulObservation)
        .addAllExceptionMessages(messages)
        .setExceptionStackTrace(stackTrace)
        .setRetryAfterMilliseconds(retryAfter.toMillis().coerceAtLeast(0))
    readModelState?.let { result.setReadModelState(it) }
    return result.build()
}

/**
 * A deferral can only be sent to a kernel that delivered a page. [EventBatch] already turns a deferral
 * into a failure for a single-partition delivery; mapping it here as well keeps a kernel that does not
 * know the state from ever being sent it.
 */
private fun PartitionState.forSinglePartition(): PartitionState =
    if (this == PartitionState.Deferred) PartitionState.Failed else this

private fun PartitionState.toReactorContract(): ObservationReactors.ObservationState = when (this) {
    PartitionState.Success -> ObservationReactors.ObservationState.Success
    PartitionState.Failed -> ObservationReactors.ObservationState.Failed
    PartitionState.Deferred -> ObservationReactors.ObservationState.Deferred
}

private fun PartitionState.toReducerContract(): ObservationReducers.ObservationState = when (this) {
    PartitionState.Success -> ObservationReducers.ObservationState.Success
    PartitionState.Failed -> ObservationReducers.ObservationState.Failed
    PartitionState.Deferred -> ObservationReducers.ObservationState.Deferred
}

private fun ObservationReactors.AppendedEvent.toAppendedEvent() = AppendedEvent(context.toEventContext(), content)

private fun ObservationReducers.AppendedEvent.toAppendedEvent() = AppendedEvent(context.toEventContext(), content)

private fun ObservationReactors.ObservationOrigin.toClient(): ObservationOrigin = when (this) {
    ObservationReactors.ObservationOrigin.Live -> ObservationOrigin.Live
    ObservationReactors.ObservationOrigin.CatchingUp -> ObservationOrigin.CatchUp
    ObservationReactors.ObservationOrigin.Replaying -> ObservationOrigin.Replay
    ObservationReactors.ObservationOrigin.Retrying -> ObservationOrigin.Retry
    else -> ObservationOrigin.Unknown
}

private fun ObservationReducers.ObservationOrigin.toClient(): ObservationOrigin = when (this) {
    ObservationReducers.ObservationOrigin.Live -> ObservationOrigin.Live
    ObservationReducers.ObservationOrigin.CatchingUp -> ObservationOrigin.CatchUp
    ObservationReducers.ObservationOrigin.Replaying -> ObservationOrigin.Replay
    ObservationReducers.ObservationOrigin.Retrying -> ObservationOrigin.Retry
    else -> ObservationOrigin.Unknown
}
