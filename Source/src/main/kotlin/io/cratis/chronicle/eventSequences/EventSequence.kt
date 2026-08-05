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
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.identity.Identity as ChronicleIdentity
import io.cratis.chronicle.identity.identityProvider
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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

    private val _appendOperations = MutableSharedFlow<List<AppendedEventWithResult>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val appendOperations: SharedFlow<List<AppendedEventWithResult>> = _appendOperations.asSharedFlow()

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        val eventType = resolveEventType(event)
        val correlationId = options?.correlationId ?: correlationIdManager.current
        val concurrencyScope = options?.concurrencyScope ?: ConcurrencyScope.none
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
            this.concurrencyScope = concurrencyScope.toContract()
        }.build()

        val response = stub.append(request)

        val result = mapAppendResponse(
            sequenceNumber = response.sequenceNumber,
            constraintViolations = response.constraintViolationsList,
            errors = response.errorsList,
            concurrencyViolation = if (response.hasConcurrencyViolation()) response.concurrencyViolation else null
        )

        emitAppendOperations(eventSourceId, listOf(event), listOf(result), correlationId, identity)

        return result
    }

    override suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions?
    ): List<AppendResult> {
        if (events.isEmpty()) return emptyList()

        val correlationId = options?.correlationId ?: correlationIdManager.current
        val concurrencyScope = options?.concurrencyScope ?: ConcurrencyScope.none

        causationManager.add(CausationType.appendManyEvents, mapOf("count" to events.size.toString()))
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity

        val eventsToAppend = events.map { event ->
            val eventType = resolveEventType(event)
            Eventsequences.EventToAppend.newBuilder().apply {
                this.eventSourceType = "Default"
                this.eventSourceId = eventSourceId
                this.eventStreamType = "Default"
                this.eventStreamId = eventSourceId
                this.eventType = eventType.toContractsEventType()
                this.content = gson.toJson(event)
                this.subject = eventSourceId
            }.build()
        }

        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.AppendManyRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.correlationId = correlationId.toContractsGuid()
            addAllEvents(eventsToAppend)
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
            putConcurrencyScopes(eventSourceId, concurrencyScope.toContract())
        }.build()

        // A single AppendMany RPC call commits all events as one atomic operation on the kernel side,
        // rather than issuing one Append RPC per event (which would neither be atomic nor efficient).
        val response = stub.appendMany(request)

        val results = mapAppendManyResponse(events.size, response)

        emitAppendOperations(eventSourceId, events, results, correlationId, identity)

        return results
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

    override suspend fun getTailSequenceNumber(eventSourceId: String?): EventSequenceNumber =
        getTailSequenceNumberInternal(eventSourceId = eventSourceId, filterEventTypes = emptyList())

    override suspend fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        eventTypes: List<KClass<*>>,
        eventStreamType: String?,
        eventStreamId: String?,
        eventSourceType: String?
    ): List<AppendedEvent> {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.GetForEventSourceIdAndEventTypesRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
            eventStreamType?.let { this.eventStreamType = it }
            eventStreamId?.let { this.eventStreamId = it }
            eventSourceType?.let { this.eventSourceType = it }
            addAllEventTypes(eventTypes.map { resolveEventTypeFor(it).toContractsEventType() })
        }.build()

        val response = stub.getForEventSourceIdAndEventTypes(request)
        return response.eventsList.map { it.toClient() }
    }

    override suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String?,
        eventTypes: List<KClass<*>>?
    ): List<AppendedEvent> {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.GetFromEventSequenceNumberRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.fromEventSequenceNumber = sequenceNumber.value
            eventSourceId?.let { this.eventSourceId = it }
            eventTypes?.let { addAllEventTypes(it.map { t -> resolveEventTypeFor(t).toContractsEventType() }) }
        }.build()

        val response = stub.getEventsFromEventSequenceNumber(request)
        return response.eventsList.map { it.toClient() }
    }

    override suspend fun getNextSequenceNumber(): EventSequenceNumber {
        val tail = getTailSequenceNumber()
        return if (tail.isUnavailable) EventSequenceNumber.first else EventSequenceNumber(tail.value + 1)
    }

    override suspend fun getTailSequenceNumberForObserver(observerType: KClass<*>): EventSequenceNumber =
        getTailSequenceNumberInternal(eventSourceId = null, filterEventTypes = resolveObserverEventTypes(observerType))

    override suspend fun completeStream(eventStreamType: String, eventStreamId: String): CompleteStreamResult {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.CompleteStreamRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventStreamType = eventStreamType
            this.eventStreamId = eventStreamId
        }.build()

        val response = stub.completeStream(request)
        if (response.isSuccess) {
            return CompleteStreamResult.Success(EventSequenceNumber(response.sequenceNumber))
        }

        return when (response.error) {
            Eventsequences.CompleteStreamError.DefaultStreamCannotBeCompleted -> CompleteStreamResult.DefaultStreamCannotBeCompleted
            else -> CompleteStreamResult.AlreadyCompleted
        }
    }

    override suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason) {
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.RedactRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.sequenceNumber = sequenceNumber.value
            this.reason = reason.value
            this.correlationId = correlationIdManager.current.toContractsGuid()
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
        }.build()

        stub.redact(request)
    }

    override suspend fun redactForEventSource(eventSourceId: String, reason: RedactionReason, eventTypes: List<KClass<*>>) {
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.RedactForEventSourceRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
            this.reason = reason.value
            addAllEventTypes(eventTypes.map { resolveEventTypeFor(it).toContractsEventType() })
            this.correlationId = correlationIdManager.current.toContractsGuid()
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
        }.build()

        stub.redactForEventSource(request)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Publishes to [appendOperations] after a completed append through this instance, whether it
     * succeeded or failed. The occurred time is approximated client-side as the server does not echo
     * it back on [Eventsequences.AppendResponse]/[Eventsequences.AppendManyResponse].
     */
    private fun emitAppendOperations(
        eventSourceId: String,
        events: List<Any>,
        results: List<AppendResult>,
        correlationId: UUID,
        causedBy: ChronicleIdentity
    ) {
        val occurred = Instant.now()
        val entries = events.mapIndexed { index, event ->
            val context = io.cratis.chronicle.events.EventContext(
                sequenceNumber = results[index].sequenceNumber.value,
                eventSourceId = eventSourceId,
                eventType = resolveEventType(event),
                occurred = occurred,
                correlationId = correlationId,
                causedBy = causedBy
            )
            AppendedEventWithResult(context, event, results[index])
        }
        _appendOperations.tryEmit(entries)
    }

    private suspend fun getTailSequenceNumberInternal(
        eventSourceId: String?,
        filterEventTypes: List<EventTypeDescriptor>
    ): EventSequenceNumber {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Eventsequences.GetTailSequenceNumberRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            eventSourceId?.let { this.eventSourceId = it }
            addAllEventTypes(filterEventTypes.map { it.toContractsEventType() })
        }.build()

        val response = stub.getTailSequenceNumber(request)
        return EventSequenceNumber(response.sequenceNumber)
    }

    /**
     * Reflects over an observer type's handler methods to find the event types it handles, the same
     * way [io.cratis.chronicle.observation.ReactorsService] discovers them for registration - the
     * second parameter of each candidate handler method, when annotated with `@EventType`.
     */
    private fun resolveObserverEventTypes(observerType: KClass<*>): List<EventTypeDescriptor> {
        val eventTypes = mutableListOf<EventTypeDescriptor>()
        for (fn in observerType.memberFunctions) {
            val params = fn.parameters
            if (params.size < 2) continue
            val eventKClass = params[1].type.classifier as? KClass<*> ?: continue
            if (eventKClass.java.getAnnotation(EventType::class.java) != null) {
                eventTypes.add(resolveEventTypeFor(eventKClass))
            }
        }
        return eventTypes.distinct()
    }

    private fun resolveEventType(event: Any): EventTypeDescriptor = resolveEventTypeFor(event::class)

    private fun resolveEventTypeFor(eventClass: KClass<*>): EventTypeDescriptor {
        val annotation = eventClass.java.getAnnotation(EventType::class.java)
        return if (annotation != null) {
            val idValue = annotation.id.ifEmpty { eventClass.java.simpleName }
            EventTypeDescriptor(
                id = io.cratis.chronicle.events.EventTypeId(idValue),
                generation = io.cratis.chronicle.events.EventTypeGeneration(annotation.generation),
                tombstone = annotation.tombstone
            )
        } else {
            EventTypeDescriptor(
                id = io.cratis.chronicle.events.EventTypeId(eventClass.java.simpleName),
                generation = io.cratis.chronicle.events.EventTypeGeneration.first
            )
        }
    }

    private fun mapAppendResponse(
        sequenceNumber: Long,
        constraintViolations: List<Eventsequences.ConstraintViolation>,
        errors: List<String>,
        concurrencyViolation: Eventsequences.ConcurrencyViolation?
    ): AppendResult {
        val mappedViolations = constraintViolations.map { it.toClient() }
        val mappedErrors = errors.map { AppendError(it) }
        val mappedConcurrencyViolation = concurrencyViolation?.toClient()

        return AppendResult(
            sequenceNumber = EventSequenceNumber(sanitizeSequenceNumber(sequenceNumber)),
            constraintViolations = mappedViolations,
            errors = mappedErrors,
            concurrencyViolation = mappedConcurrencyViolation,
            isSuccess = mappedViolations.isEmpty() && mappedErrors.isEmpty() && mappedConcurrencyViolation == null
        )
    }

    private fun mapAppendManyResponse(eventCount: Int, response: Eventsequences.AppendManyResponse): List<AppendResult> {
        val mappedViolations = response.constraintViolationsList.map { it.toClient() }
        val mappedErrors = response.errorsList.map { AppendError(it) }
        val mappedConcurrencyViolation = response.concurrencyViolationsList.firstOrNull()?.toClient()
        val isSuccess = mappedViolations.isEmpty() && mappedErrors.isEmpty() && mappedConcurrencyViolation == null
        val sequenceNumbers = response.sequenceNumbersList

        return (0 until eventCount).map { index ->
            if (isSuccess) {
                AppendResult(
                    sequenceNumber = EventSequenceNumber(sanitizeSequenceNumber(sequenceNumbers.getOrElse(index) { -1L })),
                    constraintViolations = emptyList(),
                    errors = emptyList(),
                    isSuccess = true
                )
            } else {
                AppendResult(
                    sequenceNumber = EventSequenceNumber.unavailable,
                    constraintViolations = mappedViolations,
                    errors = mappedErrors,
                    concurrencyViolation = mappedConcurrencyViolation,
                    isSuccess = false
                )
            }
        }
    }

    private fun sanitizeSequenceNumber(raw: Long): Long = if (raw == Long.MAX_VALUE || raw < 0) 0L else raw
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

private fun Eventsequences.ConstraintViolation.toClient(): ConstraintViolation = ConstraintViolation(
    constraintId = constraintName,
    message = message,
    details = detailsMap.toMap()
)

private fun Eventsequences.ConcurrencyViolation.toClient(): io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation =
    io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation(
        eventSourceId = eventSourceId,
        expectedSequenceNumber = EventSequenceNumber(expectedSequenceNumber),
        actualSequenceNumber = EventSequenceNumber(actualSequenceNumber)
    )

private fun ConcurrencyScope.toContract(): Eventsequences.ConcurrencyScope {
    val scope = this
    return Eventsequences.ConcurrencyScope.newBuilder().apply {
        this.sequenceNumber = scope.sequenceNumber.value
        this.eventSourceId = scope.eventSourceId
        scope.eventStreamType?.let { this.eventStreamType = it }
        scope.eventStreamId?.let { this.eventStreamId = it }
        scope.eventSourceType?.let { this.eventSourceType = it }
        addAllEventTypes(scope.eventTypes.map { it.toContractsEventType() })
    }.build()
}

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

private fun Bcl.Guid.toUUID(): UUID {
    // Inverse of UUID.toContractsGuid(): reverse each half back to Java's big-endian representation.
    val mostSignificantBits = java.lang.Long.reverseBytes(lo)
    val leastSignificantBits = java.lang.Long.reverseBytes(hi)
    return UUID(mostSignificantBits, leastSignificantBits)
}

private fun Eventsequences.Identity.toClient(): ChronicleIdentity = ChronicleIdentity(
    subject = subject,
    name = name,
    userName = userName,
    onBehalfOf = if (hasOnBehalfOf()) onBehalfOf.toClient() else null
)

private fun Eventsequences.EventContext.toClient(): io.cratis.chronicle.events.EventContext {
    val occurredInstant = try {
        Instant.parse(occurred.value)
    } catch (e: Exception) {
        Instant.now()
    }
    return io.cratis.chronicle.events.EventContext(
        sequenceNumber = sequenceNumber,
        eventSourceId = eventSourceId,
        eventType = EventTypeDescriptor(
            id = io.cratis.chronicle.events.EventTypeId(eventType.id),
            generation = io.cratis.chronicle.events.EventTypeGeneration(eventType.generation)
        ),
        occurred = occurredInstant,
        correlationId = correlationId.toUUID(),
        causedBy = causedBy.toClient()
    )
}

private fun Eventsequences.AppendedEvent.toClient(): AppendedEvent = AppendedEvent(
    context = context.toClient(),
    content = content
)
