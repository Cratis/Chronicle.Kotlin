// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.Sequences
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

/** Maps persisted metadata; the read request supplies the store and namespace absent from the wire context. */
internal fun Sequences.AppendedEventResponse.toClient(eventStore: String, namespace: String): AppendedEvent =
    AppendedEvent(
        context = EventContext(
            sequenceNumber = context.sequenceNumber,
            eventSourceId = context.eventSourceId,
            eventType = EventTypeDescriptor(
                id = EventTypeId(context.eventType.id),
                generation = EventTypeGeneration(context.eventType.generation),
                tombstone = context.eventType.tombstone
            ),
            occurred = context.occurred.value.toInstantOrNow(),
            correlationId = context.correlationId.toUUID(),
            causedBy = context.causedBy.toClient(),
            eventSourceType = context.eventSourceType,
            eventStreamType = context.eventStreamType,
            eventStreamId = context.eventStreamId,
            eventStore = eventStore,
            namespace = namespace,
            causation = context.causationList.map { it.toClient() },
            tags = context.tagsList.toList(),
            hash = context.hash,
            observationState = EventObservationState(context.observationStateValue),
            subject = context.subject
        ),
        content = content
    )

private fun Sequences.Causation.toClient(): Causation = Causation(
    timestamp = occurred.value.toInstantOrNow(),
    type = CausationType(type),
    properties = propertiesMap.toMap()
)

private fun Sequences.Identity.toClient(): Identity = Identity(
    subject = subject,
    name = name,
    userName = userName,
    onBehalfOf = if (hasOnBehalfOf()) onBehalfOf.toClient() else null
)

private fun Bcl.Guid.toUUID(): UUID =
    UUID(java.lang.Long.reverseBytes(lo), java.lang.Long.reverseBytes(hi))

/** Retains the existing read/observation fallback for an unparseable timestamp. */
private fun String.toInstantOrNow(): Instant = try {
    Instant.parse(this)
} catch (_: Exception) {
    Instant.now()
}
