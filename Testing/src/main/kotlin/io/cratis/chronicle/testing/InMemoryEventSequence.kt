// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.correlation.correlationIdManager
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.AppendedEventWithResult
import io.cratis.chronicle.eventSequences.CompleteStreamResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.identityProvider
import io.cratis.chronicle.json.chronicleGson
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * An event sequence that lives in a list.
 *
 * Everything an append does that matters to a spec happens here - the event type is resolved off the
 * class, the content is serialized with the client's own serializer, the position is assigned, and
 * the context is filled in the way the kernel would fill it. What does not happen is any of the
 * infrastructure: no kernel, no gRPC, no MongoDB, no container.
 *
 * The [io.cratis.chronicle.json.chronicleGson] round trip is the part worth having. An event that
 * cannot be serialized, or a concept whose adapter is missing, fails here exactly as it would
 * against a real kernel - which is most of what an append spec is actually checking.
 *
 * @param id The sequence this stands in for.
 * @param eventStoreName The event store name to stamp on event contexts.
 * @param namespace The namespace to stamp on event contexts.
 */
class InMemoryEventSequence(
    override val id: EventSequenceId = EventSequenceId.eventLog,
    private val eventStoreName: String = "testing",
    private val namespace: String = "default"
) : IEventSequence {

    private val appended = mutableListOf<AppendedEvent>()
    private val redacted = mutableSetOf<Long>()
    private val completedStreams = mutableSetOf<Pair<String, String>>()

    private val _appendOperations = MutableSharedFlow<List<AppendedEventWithResult>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val appendOperations: SharedFlow<List<AppendedEventWithResult>> = _appendOperations.asSharedFlow()

    /** Every event appended so far, oldest first. */
    val events: List<AppendedEvent> get() = appended.toList()

    /** How many events have been appended. */
    val count: Int get() = appended.size

    /** Forgets everything, so one instance can serve a whole spec class. */
    fun clear() {
        appended.clear()
        redacted.clear()
        completedStreams.clear()
    }

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        val sequenceNumber = appended.size.toLong()
        appended.add(AppendedEvent(contextFor(eventSourceId, event, sequenceNumber, options), chronicleGson.toJson(event)))

        val result = AppendResult(
            sequenceNumber = EventSequenceNumber(sequenceNumber),
            constraintViolations = emptyList(),
            errors = emptyList(),
            isSuccess = true
        )

        _appendOperations.tryEmit(
            listOf(
                AppendedEventWithResult(
                    appended.last().context,
                    event,
                    result
                )
            )
        )

        return result
    }

    override suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions?
    ): List<AppendResult> = events.map { append(eventSourceId, it, options) }

    override suspend fun appendMany(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope>,
        correlationId: UUID?
    ): List<AppendResult> = events.map { append(it.eventSourceId, it.event, it.toTestAppendOptions(correlationId)) }

    override suspend fun hasEventsFor(eventSourceId: String): Boolean =
        appended.any { it.context.eventSourceId == eventSourceId }

    override suspend fun getTailSequenceNumber(eventSourceId: String?): EventSequenceNumber {
        val forSource = appended.filter { eventSourceId == null || it.context.eventSourceId == eventSourceId }
        return if (forSource.isEmpty()) {
            EventSequenceNumber.unset
        } else {
            EventSequenceNumber(forSource.last().context.sequenceNumber)
        }
    }

    override suspend fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        eventTypes: List<KClass<*>>,
        eventStreamType: String?,
        eventStreamId: String?,
        eventSourceType: String?
    ): List<AppendedEvent> {
        val wanted = eventTypes.map { it.eventTypeId() }.toSet()
        return appended.filter {
            it.context.eventSourceId == eventSourceId && it.context.eventType.id.value in wanted
        }
    }

    override suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String?,
        eventTypes: List<KClass<*>>?
    ): List<AppendedEvent> {
        val wanted = eventTypes?.map { it.eventTypeId() }?.toSet()
        return appended.filter { event ->
            event.context.sequenceNumber >= sequenceNumber.value &&
                (eventSourceId == null || event.context.eventSourceId == eventSourceId) &&
                (wanted == null || event.context.eventType.id.value in wanted)
        }
    }

    override suspend fun getNextSequenceNumber(): EventSequenceNumber =
        EventSequenceNumber(appended.size.toLong())

    override suspend fun getTailSequenceNumberForObserver(observerType: KClass<*>): EventSequenceNumber =
        getTailSequenceNumber()

    override suspend fun completeStream(
        eventStreamType: String,
        eventStreamId: String
    ): CompleteStreamResult = when {
        eventStreamType == AppendOptionsDefaults.EVENT_STREAM_TYPE ->
            CompleteStreamResult.DefaultStreamCannotBeCompleted

        !completedStreams.add(eventStreamType to eventStreamId) ->
            CompleteStreamResult.AlreadyCompleted

        else -> CompleteStreamResult.Success(getTailSequenceNumber())
    }

    override suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason) {
        redacted.add(sequenceNumber.value)
    }

    override suspend fun redactForEventSource(
        eventSourceId: String,
        reason: RedactionReason,
        eventTypes: List<KClass<*>>
    ) {
        val wanted = eventTypes.map { it.eventTypeId() }.toSet()
        appended
            .filter { it.context.eventSourceId == eventSourceId }
            .filter { wanted.isEmpty() || it.context.eventType.id.value in wanted }
            .forEach { redacted.add(it.context.sequenceNumber) }
    }

    /** Whether the event at [sequenceNumber] has been redacted. */
    fun isRedacted(sequenceNumber: Long): Boolean = sequenceNumber in redacted

    private fun contextFor(
        eventSourceId: String,
        event: Any,
        sequenceNumber: Long,
        options: AppendOptions?
    ): EventContext {
        val annotation = event::class.findAnnotation<EventType>()
            ?: throw IllegalArgumentException(
                "'${event::class.simpleName}' is not an event type - annotate it with @EventType, " +
                    "which is what the kernel would insist on too"
            )

        return EventContext(
            sequenceNumber = sequenceNumber,
            eventSourceId = eventSourceId,
            eventType = EventTypeDescriptor(
                EventTypeId(event::class.eventTypeId()),
                EventTypeGeneration(annotation.generation),
                annotation.tombstone
            ),
            occurred = options?.occurred ?: Instant.now(),
            correlationId = options?.correlationId ?: correlationIdManager.current,
            causedBy = identityProvider.currentIdentity,
            eventSourceType = options?.eventSourceType ?: AppendOptionsDefaults.EVENT_SOURCE_TYPE,
            eventStreamType = options?.eventStreamType ?: AppendOptionsDefaults.EVENT_STREAM_TYPE,
            eventStreamId = options?.eventStreamId ?: eventSourceId,
            eventStore = eventStoreName,
            namespace = namespace,
            causation = options?.causation?.ifEmpty { null } ?: causationManager.currentChain,
            tags = options?.tags ?: emptyList()
        )
    }

    private fun EventForEventSourceId.toTestAppendOptions(correlationId: UUID?) = AppendOptions(
        correlationId = correlationId,
        eventSourceType = eventSourceType,
        eventStreamType = eventStreamType,
        eventStreamId = eventStreamId,
        subject = subject,
        tags = tags,
        occurred = occurred,
        causation = causation
    )
}

/** The defaults the kernel applies when an append does not name them. */
internal object AppendOptionsDefaults {
    const val EVENT_SOURCE_TYPE = "Default"
    const val EVENT_STREAM_TYPE = "Default"
}

/** The identifier an event type is registered under: its declared id, or the class simple name. */
internal fun KClass<*>.eventTypeId(): String {
    val annotation = findAnnotation<EventType>()
        ?: throw IllegalArgumentException("'$simpleName' is not annotated with @EventType")
    return annotation.id.ifEmpty { simpleName!! }
}
