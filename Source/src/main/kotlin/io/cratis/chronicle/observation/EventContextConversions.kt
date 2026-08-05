// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import bcl.Bcl
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import java.time.Instant
import java.util.UUID

/**
 * Conversions from the generated contract types to the client's own [EventContext].
 *
 * Reactors and reducers each get their own generated `EventContext` from the kernel. The two are
 * structurally identical but unrelated JVM types, so each needs its own mapping - only the scalar
 * helpers below are shared.
 */

/** Converts the reactor contract's event context into an [EventContext]. */
internal fun ObservationReactors.EventContext.toEventContext(): EventContext = EventContext(
    sequenceNumber = sequenceNumber,
    eventSourceId = eventSourceId,
    eventType = EventTypeDescriptor(
        id = EventTypeId(eventType.id),
        generation = EventTypeGeneration(eventType.generation)
    ),
    occurred = occurred.value.toInstantOrNow(),
    correlationId = if (hasCorrelationId()) correlationId.toUuid() else EMPTY_UUID,
    causedBy = if (hasCausedBy()) causedBy.toIdentity() else Identity.unknown,
    eventSourceType = eventSourceType,
    eventStreamType = eventStreamType,
    eventStreamId = eventStreamId,
    eventStore = eventStore,
    namespace = namespace,
    causation = causationList.map { it.toCausation() },
    tags = tagsList.toList(),
    hash = hash,
    observationState = EventObservationState(observationStateValue)
)

/** Converts the reducer contract's event context into an [EventContext]. */
internal fun ObservationReducers.EventContext.toEventContext(): EventContext = EventContext(
    sequenceNumber = sequenceNumber,
    eventSourceId = eventSourceId,
    eventType = EventTypeDescriptor(
        id = EventTypeId(eventType.id),
        generation = EventTypeGeneration(eventType.generation)
    ),
    occurred = occurred.value.toInstantOrNow(),
    correlationId = if (hasCorrelationId()) correlationId.toUuid() else EMPTY_UUID,
    causedBy = if (hasCausedBy()) causedBy.toIdentity() else Identity.unknown,
    eventSourceType = eventSourceType,
    eventStreamType = eventStreamType,
    eventStreamId = eventStreamId,
    eventStore = eventStore,
    namespace = namespace,
    causation = causationList.map { it.toCausation() },
    tags = tagsList.toList(),
    hash = hash,
    observationState = EventObservationState(observationStateValue)
)

private fun ObservationReactors.Identity.toIdentity(): Identity = Identity(
    subject = subject,
    name = name,
    userName = userName,
    onBehalfOf = if (hasOnBehalfOf()) onBehalfOf.toIdentity() else null
)

private fun ObservationReducers.Identity.toIdentity(): Identity = Identity(
    subject = subject,
    name = name,
    userName = userName,
    onBehalfOf = if (hasOnBehalfOf()) onBehalfOf.toIdentity() else null
)

private fun ObservationReactors.Causation.toCausation(): Causation = Causation(
    timestamp = occurred.value.toInstantOrNow(),
    type = CausationType(type),
    properties = propertiesMap.toMap()
)

private fun ObservationReducers.Causation.toCausation(): Causation = Causation(
    timestamp = occurred.value.toInstantOrNow(),
    type = CausationType(type),
    properties = propertiesMap.toMap()
)

/**
 * Reads a `bcl.Guid` as a [UUID].
 *
 * `bcl.Guid` stores `lo` as the first 8 bytes and `hi` as the second 8, little-endian, whereas
 * [UUID] is big-endian in both halves - so each half is byte-reversed on the way across.
 */
private fun Bcl.Guid.toUuid(): UUID =
    UUID(java.lang.Long.reverseBytes(lo), java.lang.Long.reverseBytes(hi))

/**
 * Parses an ISO-8601 timestamp, falling back to now when the kernel sent something unparseable.
 * An unreadable timestamp is not worth failing an observation over.
 */
private fun String.toInstantOrNow(): Instant = try {
    Instant.parse(this)
} catch (_: Exception) {
    Instant.now()
}

/** The correlation identifier used when the kernel did not send one. */
private val EMPTY_UUID: UUID = UUID(0L, 0L)
