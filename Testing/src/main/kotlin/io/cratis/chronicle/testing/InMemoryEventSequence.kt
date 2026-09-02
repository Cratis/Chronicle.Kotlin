// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.OperationContext
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
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.json.chronicleGson
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
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
 * against a real kernel - which is most of what an append spec is actually checking. Batch input
 * and the supported concurrency scopes are validated before the backing list is changed, so a
 * rejected batch leaves no residue.
 *
 * Concurrency checks model the persisted event-context dimensions represented by [ConcurrencyScope].
 * They intentionally do not model kernel-side constraints or concurrent distributed writers; use
 * the real-kernel compatibility tests for those infrastructure-dependent behaviors.
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

    private data class AppendBatchOutcome(
        val results: List<AppendResult>,
        val emitted: List<AppendedEventWithResult>
    )

    private val appended = mutableListOf<AppendedEvent>()
    private val redacted = mutableSetOf<Long>()
    private val completedStreams = mutableSetOf<Pair<String, String>>()

    private val _appendOperations = MutableSharedFlow<List<AppendedEventWithResult>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val appendOperations: SharedFlow<List<AppendedEventWithResult>> = _appendOperations.asSharedFlow()

    /** Every event appended so far, oldest first. */
    val events: List<AppendedEvent> get() = synchronized(this) { appended.toList() }

    /** How many events have been appended. */
    val count: Int get() = synchronized(this) { appended.size }

    /** Forgets everything, so one instance can serve a whole spec class. */
    fun clear() = synchronized(this) {
        appended.clear()
        redacted.clear()
        completedStreams.clear()
    }

    override suspend fun append(
        eventSourceId: String,
        event: Any,
        context: OperationContext,
        options: AppendOptions?
    ): AppendResult = appendBatch(
        listOf(EventForEventSourceId(eventSourceId, event).withOptions(options)),
        context,
        options?.concurrencyScope?.let { mapOf(eventSourceId to it) }.orEmpty()
    ).single()

    override suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        context: OperationContext,
        options: AppendOptions?
    ): List<AppendResult> = appendBatch(
        events.map { EventForEventSourceId(eventSourceId, it).withOptions(options) },
        context,
        options?.concurrencyScope?.let { mapOf(eventSourceId to it) }.orEmpty()
    )

    override suspend fun appendMany(
        events: List<EventForEventSourceId>,
        context: OperationContext,
        concurrencyScopes: Map<String, ConcurrencyScope>
    ): List<AppendResult> = appendBatch(events, context, concurrencyScopes)

    override suspend fun hasEventsFor(eventSourceId: String): Boolean = synchronized(this) {
        appended.any { it.context.eventSourceId == eventSourceId }
    }

    override suspend fun getTailSequenceNumber(eventSourceId: String?): EventSequenceNumber = synchronized(this) {
        val forSource = appended.filter { eventSourceId == null || it.context.eventSourceId == eventSourceId }
        if (forSource.isEmpty()) {
            EventSequenceNumber.unavailable
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
    ): List<AppendedEvent> = synchronized(this) {
        val wanted = eventTypes.map { it.eventTypeId() }.toSet()
        appended.filter {
            it.context.eventSourceId == eventSourceId && it.context.eventType.id.value in wanted
        }
    }

    override suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String?,
        eventTypes: List<KClass<*>>?,
        tags: List<String>
    ): List<AppendedEvent> = synchronized(this) {
        val wanted = eventTypes?.map { it.eventTypeId() }?.toSet()
        appended.filter { event ->
            event.context.sequenceNumber >= sequenceNumber.value &&
                (eventSourceId == null || event.context.eventSourceId == eventSourceId) &&
                (wanted == null || event.context.eventType.id.value in wanted) &&
                (tags.isEmpty() || event.context.tags.any { it in tags })
        }
    }

    override suspend fun getNextSequenceNumber(): EventSequenceNumber = synchronized(this) {
        EventSequenceNumber(appended.size.toLong())
    }

    override suspend fun getTailSequenceNumberForObserver(observerType: KClass<*>): EventSequenceNumber =
        getTailSequenceNumber()

    override suspend fun completeStream(
        eventStreamType: String,
        eventStreamId: String
    ): CompleteStreamResult = synchronized(this) {
        when {
            eventStreamType == AppendOptionsDefaults.EVENT_STREAM_TYPE ->
                CompleteStreamResult.DefaultStreamCannotBeCompleted

            !completedStreams.add(eventStreamType to eventStreamId) ->
                CompleteStreamResult.AlreadyCompleted

            else -> CompleteStreamResult.Success(
                appended.lastOrNull()
                    ?.let { EventSequenceNumber(it.context.sequenceNumber) }
                    ?: EventSequenceNumber.unavailable
            )
        }
    }

    override suspend fun redact(
        sequenceNumber: EventSequenceNumber,
        reason: RedactionReason,
        context: OperationContext
    ) {
        synchronized(this) { redacted.add(sequenceNumber.value) }
    }

    override suspend fun redactForEventSource(
        eventSourceId: String,
        reason: RedactionReason,
        context: OperationContext,
        eventTypes: List<KClass<*>>
    ) {
        synchronized(this) {
            val wanted = eventTypes.map { it.eventTypeId() }.toSet()
            appended
                .filter { it.context.eventSourceId == eventSourceId }
                .filter { wanted.isEmpty() || it.context.eventType.id.value in wanted }
                .forEach { redacted.add(it.context.sequenceNumber) }
        }
    }

    /** Whether the event at [sequenceNumber] has been redacted. */
    fun isRedacted(sequenceNumber: Long): Boolean = synchronized(this) { sequenceNumber in redacted }

    private fun appendBatch(
        events: List<EventForEventSourceId>,
        operationContext: OperationContext,
        concurrencyScopes: Map<String, ConcurrencyScope>
    ): List<AppendResult> {
        if (events.isEmpty()) return emptyList()

        val outcome = synchronized(this) {
            val firstSequenceNumber = appended.size.toLong()
            val prepared = events.mapIndexed { index, event ->
                AppendedEvent(
                    contextFor(
                        event.eventSourceId,
                        event.event,
                        firstSequenceNumber + index,
                        operationContext,
                        event.toTestAppendOptions()
                    ),
                    chronicleGson.toJson(event.event)
                )
            }
            val violations = concurrencyViolationsFor(concurrencyScopes)
            val concurrencyCheckPerformed = concurrencyScopes.values.any {
                it != ConcurrencyScope.none && it != ConcurrencyScope.notSet
            }

            if (violations.isNotEmpty()) {
                val failedResults = List(events.size) {
                    AppendResult(
                        sequenceNumber = EventSequenceNumber.unavailable,
                        constraintViolations = emptyList(),
                        errors = emptyList(),
                        isSuccess = false,
                        concurrencyViolations = violations,
                        concurrencyCheckPerformed = concurrencyCheckPerformed
                    )
                }
                AppendBatchOutcome(
                    failedResults,
                    events.indices.map { index ->
                        AppendedEventWithResult(
                            prepared[index].context.copy(sequenceNumber = EventSequenceNumber.unavailable.value),
                            events[index].event,
                            failedResults[index]
                        )
                    }
                )
            } else {
                val successfulResults = prepared.map { preparedEvent ->
                    AppendResult(
                        sequenceNumber = EventSequenceNumber(preparedEvent.context.sequenceNumber),
                        constraintViolations = emptyList(),
                        errors = emptyList(),
                        isSuccess = true,
                        concurrencyCheckPerformed = concurrencyCheckPerformed
                    )
                }
                appended.addAll(prepared)
                AppendBatchOutcome(
                    successfulResults,
                    events.indices.map { index ->
                        AppendedEventWithResult(prepared[index].context, events[index].event, successfulResults[index])
                    }
                )
            }
        }

        _appendOperations.tryEmit(outcome.emitted)
        return outcome.results
    }

    private fun concurrencyViolationsFor(
        concurrencyScopes: Map<String, ConcurrencyScope>
    ): List<ConcurrencyViolation> = concurrencyScopes.mapNotNull { (eventSourceId, scope) ->
        if (scope == ConcurrencyScope.none || scope == ConcurrencyScope.notSet) return@mapNotNull null

        val actual = appended.asSequence()
            .filter { event -> !scope.eventSourceId || event.context.eventSourceId == eventSourceId }
            .filter { event -> scope.eventStreamType == null || event.context.eventStreamType == scope.eventStreamType }
            .filter { event -> scope.eventStreamId == null || event.context.eventStreamId == scope.eventStreamId }
            .filter { event -> scope.eventSourceType == null || event.context.eventSourceType == scope.eventSourceType }
            .filter { event ->
                scope.eventTypes.isEmpty() || scope.eventTypes.any { it.id == event.context.eventType.id }
            }
            .maxByOrNull { it.context.sequenceNumber }
            ?.let { EventSequenceNumber(it.context.sequenceNumber) }
            ?: EventSequenceNumber.unavailable

        val violated = if (scope.expectsNoMatchingEvent) {
            actual.isActualValue
        } else {
            actual != scope.sequenceNumber
        }
        if (violated) {
            val expected = if (scope.expectsNoMatchingEvent) EventSequenceNumber.unavailable else scope.sequenceNumber
            ConcurrencyViolation(eventSourceId, expected, actual)
        } else {
            null
        }
    }

    private fun EventForEventSourceId.withOptions(options: AppendOptions?) = copy(
        eventStreamType = options?.eventStreamType,
        eventStreamId = options?.eventStreamId,
        eventSourceType = options?.eventSourceType,
        tags = options?.tags ?: emptyList(),
        occurred = options?.occurred,
        subject = options?.subject
    )

    private fun contextFor(
        eventSourceId: String,
        event: Any,
        sequenceNumber: Long,
        operationContext: OperationContext,
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
            correlationId = operationContext.correlationId,
            causedBy = operationContext.causedBy,
            eventSourceType = options?.eventSourceType ?: AppendOptionsDefaults.EVENT_SOURCE_TYPE,
            eventStreamType = options?.eventStreamType ?: AppendOptionsDefaults.EVENT_STREAM_TYPE,
            eventStreamId = options?.eventStreamId ?: eventSourceId,
            eventStore = eventStoreName,
            namespace = namespace,
            causation = operationContext.causation,
            tags = options?.tags ?: emptyList()
        )
    }

    private fun EventForEventSourceId.toTestAppendOptions() = AppendOptions(
        eventSourceType = eventSourceType,
        eventStreamType = eventStreamType,
        eventStreamId = eventStreamId,
        subject = subject,
        tags = tags,
        occurred = occurred
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
