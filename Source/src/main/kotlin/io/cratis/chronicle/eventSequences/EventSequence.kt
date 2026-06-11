// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.Eventsequences
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import bcl.Bcl
import com.google.gson.Gson
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.correlation.correlationIdManager
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.identity.Identity as ChronicleIdentity
import io.cratis.chronicle.identity.identityProvider
import java.time.format.DateTimeFormatter
import java.util.UUID

private val gson = Gson()

/**
 * Implements [IEventSequence] by communicating with the Chronicle Kernel via gRPC.
 */
open class EventSequence(
    override val id: EventSequenceId,
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: EventSequencesGrpcKt.EventSequencesCoroutineStub
) : IEventSequence {

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        val eventType = resolveEventType(event)
        val correlationId = options?.correlationId ?: correlationIdManager.current
        val content = gson.toJson(event)

        causationManager.add(CausationType.appendEvent, mapOf("eventType" to eventType.id.value))
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity

        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.AppendRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.correlationId = correlationId.toContractsGuid()
            this.eventSourceType = "Default"
            this.eventSourceId = eventSourceId
            this.eventStreamType = "Default"
            this.eventStreamId = eventSourceId
            this.eventType = eventType.toContractsEventType()
            this.content = content
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
            this.subject = eventSourceId
            // sequenceNumber = -1L encodes as ulong.MaxValue (Unavailable) — disables concurrency validation on the server.
            this.concurrencyScope = Eventsequences.ConcurrencyScope.newBuilder()
                .setSequenceNumber(-1L)
                .build()
        }.build()

        val response = stub.append(request)

        return mapAppendResponse(
            sequenceNumber = response.sequenceNumber,
            constraintViolations = response.constraintViolationsList,
            errors = response.errorsList
        )
    }

    override suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions?
    ): List<AppendResult> {
        if (events.isEmpty()) return emptyList()
        causationManager.add(CausationType.appendManyEvents, mapOf("count" to events.size.toString()))
        return events.map { event -> append(eventSourceId, event, options) }
    }

    override suspend fun hasEventsFor(eventSourceId: String): Boolean {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.HasEventsForEventSourceIdRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
        }.build()

        val response = stub.hasEventsForEventSourceId(request)
        return response.hasEvents
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun resolveEventType(event: Any): EventTypeDescriptor {
        val annotation = event::class.java.getAnnotation(EventType::class.java)
        return if (annotation != null) {
            val idValue = annotation.id.ifEmpty { event::class.java.simpleName }
            EventTypeDescriptor(
                id = io.cratis.chronicle.events.EventTypeId(idValue),
                generation = io.cratis.chronicle.events.EventTypeGeneration(annotation.generation),
                tombstone = annotation.tombstone
            )
        } else {
            EventTypeDescriptor(
                id = io.cratis.chronicle.events.EventTypeId(event::class.java.simpleName),
                generation = io.cratis.chronicle.events.EventTypeGeneration.first
            )
        }
    }

    private fun mapAppendResponse(
        sequenceNumber: Long,
        constraintViolations: List<Eventsequences.ConstraintViolation>,
        errors: List<String>
    ): AppendResult {
        val safeSequenceNumber = if (sequenceNumber == Long.MAX_VALUE || sequenceNumber < 0) 0L else sequenceNumber

        val mappedViolations = constraintViolations.map { v ->
            ConstraintViolation(
                constraintId = v.constraintName,
                message = v.message,
                details = v.detailsMap.toMap()
            )
        }

        val mappedErrors = errors.map { AppendError(it) }

        return AppendResult(
            sequenceNumber = EventSequenceNumber(safeSequenceNumber),
            constraintViolations = mappedViolations,
            errors = mappedErrors,
            isSuccess = mappedViolations.isEmpty() && mappedErrors.isEmpty()
        )
    }
}

// -------------------------------------------------------------------------
// Extension functions for proto conversion
// -------------------------------------------------------------------------

private fun UUID.toContractsGuid(): Bcl.Guid {
    // bcl.Guid: lo = first 8 bytes, hi = second 8 bytes, little-endian.
    // Java UUID.mostSignificantBits and leastSignificantBits are big-endian, so reverse each half.
    return Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
        .build()
}

private fun EventTypeDescriptor.toContractsEventType(): Eventsequences.EventType =
    Eventsequences.EventType.newBuilder()
        .setId(id.value)
        .setGeneration(generation.value)
        .setTombstone(tombstone)
        .build()

private fun io.cratis.chronicle.auditing.Causation.toContractsCausation(): Eventsequences.Causation =
    Eventsequences.Causation.newBuilder()
        .setOccurred(
            Eventsequences.SerializableDateTimeOffset.newBuilder()
                .setValue(DateTimeFormatter.ISO_INSTANT.format(timestamp))
                .build()
        )
        .setType(type.name)
        .putAllProperties(properties)
        .build()

private fun ChronicleIdentity.toContractsIdentity(): Eventsequences.Identity {
    val builder = Eventsequences.Identity.newBuilder()
        .setSubject(subject)
        .setName(name)
        .setUserName(userName)
    onBehalfOf?.let { builder.setOnBehalfOf(it.toContractsIdentity()) }
    return builder.build()
}
