// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.identity.Identity as ChronicleIdentity
import java.time.Instant
import java.time.format.DateTimeFormatter

// Extension functions for proto conversion and command/query envelope unwrapping.

@JvmSynthetic
internal fun Instant.toContractsDateTimeOffset(): Sequences.SerializableDateTimeOffset =
    Sequences.SerializableDateTimeOffset.newBuilder()
        .setValue(DateTimeFormatter.ISO_INSTANT.format(this))
        .build()

@JvmSynthetic
internal fun EventTypeDescriptor.toContractsEventType(): Sequences.EventType =
    Sequences.EventType.newBuilder()
        .setId(id.value)
        .setGeneration(generation.value)
        .setTombstone(tombstone)
        .build()

@JvmSynthetic
internal fun Sequences.ConstraintViolation.toClient(): ConstraintViolation = ConstraintViolation(
    constraintId = constraintName,
    message = message,
    details = detailsMap.toMap()
)

@JvmSynthetic
internal fun Sequences.ConcurrencyViolation.toClient(): io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation =
    io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation(
        eventSourceId = eventSourceId,
        expectedSequenceNumber = EventSequenceNumber(expectedSequenceNumber),
        actualSequenceNumber = EventSequenceNumber(actualSequenceNumber)
    )

@JvmSynthetic
internal fun ConcurrencyScope.toContract(): Sequences.ConcurrencyScope {
    val scope = this
    return Sequences.ConcurrencyScope.newBuilder().apply {
        this.sequenceNumber = scope.sequenceNumber.value
        this.eventSourceId = scope.eventSourceId
        scope.eventStreamType?.let { this.eventStreamType = it }
        scope.eventStreamId?.let { this.eventStreamId = it }
        scope.eventSourceType?.let { this.eventSourceType = it }
        addAllEventTypes(scope.eventTypes.map { it.toContractsEventType() })
        this.expectsNoMatchingEvent = scope.expectsNoMatchingEvent
    }.build()
}

@JvmSynthetic
internal fun io.cratis.chronicle.auditing.Causation.toContractsCausation(): Sequences.Causation =
    Sequences.Causation.newBuilder()
        .setOccurred(
            Sequences.SerializableDateTimeOffset.newBuilder()
                .setValue(DateTimeFormatter.ISO_INSTANT.format(timestamp))
                .build()
        )
        .setType(type.name)
        .putAllProperties(properties)
        .build()

@JvmSynthetic
internal fun ChronicleIdentity.toContractsIdentity(): Sequences.Identity {
    val builder = Sequences.Identity.newBuilder()
        .setSubject(subject)
        .setName(name)
        .setUserName(userName)
    onBehalfOf?.let { builder.setOnBehalfOf(it.toContractsIdentity()) }
    return builder.build()
}

// -------------------------------------------------------------------------
// Command/query envelope unwrapping
// -------------------------------------------------------------------------
//
// A successful envelope always carries its Response/Data - the wire type only marks it optional
// because protobuf gives every singular message field a technically-absent state.

private fun ensureSuccessMessage(operation: String, isAuthorized: Boolean, authorizationFailureReason: String, exceptionMessages: List<String>) {
    if (!isAuthorized) throw ChronicleCommandRejected(operation, authorizationFailureReason.ifEmpty { "not authorized" })
    if (exceptionMessages.isNotEmpty()) throw ChronicleCommandRejected(operation, exceptionMessages.joinToString("; "))
}

@JvmSynthetic
internal fun Sequences.CommandResult.ensureSuccess(operation: String) =
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)

@JvmSynthetic
internal fun Sequences.CommandResult_AppendResponse.ensureSuccess(operation: String): Sequences.AppendResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

@JvmSynthetic
internal fun Sequences.CommandResult_AppendManyResponse.ensureSuccess(operation: String): Sequences.AppendManyResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

@JvmSynthetic
internal fun Sequences.CommandResult_CompleteStreamResponse.ensureSuccess(operation: String): Sequences.CompleteStreamResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

@JvmSynthetic
internal fun Sequences.QueryResult_EventSourceEventsResponse.ensureSuccess(operation: String): Sequences.EventSourceEventsResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return data
}

@JvmSynthetic
internal fun Sequences.QueryResult_EventSequenceTailResponse.ensureSuccess(operation: String): Sequences.EventSequenceTailResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return data
}

@JvmSynthetic
internal fun Sequences.QueryResult_IEnumerable_AppendedEventResponse.ensureSuccess(operation: String): Sequences.QueryResult_IEnumerable_AppendedEventResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return this
}
