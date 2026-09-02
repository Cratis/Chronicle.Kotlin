// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import kotlinx.coroutines.flow.SharedFlow
import kotlin.reflect.KClass

/**
 * Defines the API surface for an event sequence.
 */
interface IEventSequence {
    /** The unique identifier of this event sequence. */
    val id: EventSequenceId

    /**
     * A hot [SharedFlow] that emits a list of [AppendedEventWithResult] after each append operation
     * made through this specific [IEventSequence] instance.
     *
     * A single-event [append] emits a list of one element; a batch [appendMany] emits the full
     * batch. Subscribers receive the emission after the operation has completed, whether it
     * succeeded or failed. Explicit [io.cratis.chronicle.transactions.UnitOfWork] commits emit
     * through the same sequence because they perform one ordinary append-many operation.
     */
    val appendOperations: SharedFlow<List<AppendedEventWithResult>>

    /**
     * Appends a single event to the event sequence.
     *
     * @param eventSourceId The identifier of the event source.
     * @param event The event object to append. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
     * @param options Optional [AppendOptions].
     * @return The [AppendResult] of the operation.
     */
    suspend fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        append(eventSourceId, event, OperationContext.system(), options)

    /** Appends one event using the supplied immutable operation metadata. */
    suspend fun append(
        eventSourceId: String,
        event: Any,
        context: OperationContext,
        options: AppendOptions? = null
    ): AppendResult

    /**
     * Appends multiple events for a single event source to the event sequence.
     *
     * @param eventSourceId The identifier of the event source.
     * @param events The event objects to append.
     * @param options Optional [AppendOptions].
     * @return A list of [AppendResult], one per event.
     */
    suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult> =
        appendMany(eventSourceId, events, OperationContext.system(), options)

    /** Appends events for one source as an atomic batch using [context] for the whole batch. */
    suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        context: OperationContext,
        options: AppendOptions? = null
    ): List<AppendResult>

    /**
     * Appends events spanning any number of event sources as a single atomic batch.
     *
     * Use this when a batch has to commit as one unit across more than one event source - the
     * single-event-source overload cannot express that, and appending per source would give up
     * atomicity. Each [EventForEventSourceId] carries its own shaping, so events in the same batch can go to
     * different streams.
     *
     * @param events The events to append, in the order they should be appended.
     * @param concurrencyScopes Optional [ConcurrencyScope] per event source id. Only the event
     *   sources present in the map are concurrency checked; any source left out is appended
     *   unchecked.
     * @return A list of [AppendResult], one per event, in the order of [events].
     */
    suspend fun appendMany(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap()
    ): List<AppendResult> = appendMany(events, OperationContext.system(), concurrencyScopes)

    /** Appends a multi-source atomic batch using one [context] for every event in the batch. */
    suspend fun appendMany(
        events: List<EventForEventSourceId>,
        context: OperationContext,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap()
    ): List<AppendResult>

    /**
     * Determines whether there are any events for a given event source identifier.
     *
     * @param eventSourceId The event source identifier to check.
     * @return `true` if events exist for the given source, otherwise `false`.
     */
    suspend fun hasEventsFor(eventSourceId: String): Boolean

    /**
     * Gets the sequence number of the last (tail) event in the sequence.
     *
     * @param eventSourceId Optional event source identifier to get the tail for.
     *   If not specified, the tail sequence number across all event sources is returned.
     * @return The tail [EventSequenceNumber].
     */
    suspend fun getTailSequenceNumber(eventSourceId: String? = null): EventSequenceNumber

    /**
     * Gets all events for a specific event source, optionally filtered and narrowed further.
     *
     * @param eventSourceId The event source identifier to get events for.
     * @param eventTypes The event types to filter for.
     * @param eventStreamType Optional event stream type to narrow to. Defaults to all stream types.
     * @param eventStreamId Optional event stream identifier to narrow to. Defaults to all streams.
     * @param eventSourceType Optional event source type to narrow to. Defaults to all source types.
     * @return A list of [AppendedEvent].
     */
    suspend fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        eventTypes: List<KClass<*>>,
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null
    ): List<AppendedEvent>

    /**
     * Gets all events after and including the given sequence number, with optional narrowing.
     *
     * @param sequenceNumber The [EventSequenceNumber] of the first event to get from.
     * @param eventSourceId Optional event source identifier to filter by.
     * @param eventTypes Optional event types to filter by.
     * @param tags Optional tags to filter by.
     * @return A list of [AppendedEvent].
     */
    suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String? = null,
        eventTypes: List<KClass<*>>? = null,
        tags: List<String> = emptyList()
    ): List<AppendedEvent>

    /**
     * Gets the sequence number that will be assigned to the next appended event.
     *
     * @return The next [EventSequenceNumber].
     */
    suspend fun getNextSequenceNumber(): EventSequenceNumber

    /**
     * Gets the sequence number of the last (tail) event relevant to a specific observer type.
     *
     * @param observerType The reactor or reducer type to get the tail for.
     * @return The tail [EventSequenceNumber], based on the tail of the event types the observer handles.
     */
    suspend fun getTailSequenceNumberForObserver(observerType: KClass<*>): EventSequenceNumber

    /**
     * Completes a stream so that no further events can be appended to it.
     *
     * @param eventStreamType The event stream type identifying the stream's type.
     * @param eventStreamId The event stream id identifying the stream within the type.
     * @return A [CompleteStreamResult] — [CompleteStreamResult.Success] with the tail sequence number
     *   on success, or one of the error cases describing why the operation was rejected.
     * @remarks The default stream — event stream type `"Default"` paired with the default event stream
     *   id — can never be completed. Completing an already-completed stream returns
     *   [CompleteStreamResult.AlreadyCompleted] and leaves the stream in its completed state.
     */
    suspend fun completeStream(eventStreamType: String, eventStreamId: String): CompleteStreamResult

    /**
     * Redacts a specific event instance, permanently rewriting its content.
     *
     * This is a destructive content rewrite, not a field mask - the original content is gone
     * once this returns. Use it for compliance-driven removal of a single event's payload.
     *
     * @param sequenceNumber The [EventSequenceNumber] of the event to redact.
     * @param reason The [RedactionReason] for redacting.
     */
    suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason) =
        redact(sequenceNumber, reason, OperationContext.system())

    /** Redacts one event using explicit operation metadata. */
    suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason, context: OperationContext)

    /**
     * Redacts all events for a specific event source, optionally filtered to specific event types.
     *
     * @param eventSourceId The event source identifier to redact events for.
     * @param reason The [RedactionReason] for redacting.
     * @param eventTypes Optional event types to narrow the redaction to. If empty, all event types
     *   for the event source are redacted.
     */
    suspend fun redactForEventSource(
        eventSourceId: String,
        reason: RedactionReason,
        eventTypes: List<KClass<*>> = emptyList()
    ) = redactForEventSource(eventSourceId, reason, OperationContext.system(), eventTypes)

    /** Redacts matching events using explicit operation metadata. */
    suspend fun redactForEventSource(
        eventSourceId: String,
        reason: RedactionReason,
        context: OperationContext,
        eventTypes: List<KClass<*>> = emptyList()
    )
}
