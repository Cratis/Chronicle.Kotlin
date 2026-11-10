// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.imports.IEventImport
import io.cratis.chronicle.imports.ImportId
import io.cratis.chronicle.imports.ImportOptions
import io.cratis.chronicle.imports.ImportProgress
import io.cratis.chronicle.imports.ImportResult
import io.cratis.chronicle.imports.ImportsNotSupported
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
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
     * succeeded or failed. This flow does not emit for transactional appends through
     * [ITransactionalEventSequence].
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
    suspend fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult

    /**
     * Appends multiple events for a single event source to the event sequence.
     *
     * @param eventSourceId The identifier of the event source.
     * @param events The event objects to append.
     * @param options Optional [AppendOptions].
     * @return A list of [AppendResult], one per event.
     */
    suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult>

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
     * @param correlationId Optional correlation identifier for the whole batch.
     *   Defaults to the current [io.cratis.chronicle.correlation.CorrelationIdManager] value.
     * @return A list of [AppendResult], one per event, in the order of [events].
     */
    suspend fun appendMany(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap(),
        correlationId: UUID? = null
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
     * Narrowing is opt-in: a missing or empty route means "do not narrow", which is how the kernel
     * reads an unspecified filter. That matters because the kernel resolves an append that supplied
     * no routing onto `Default`/`All`/`Default` - narrowing a read to a legacy `Default` stream type
     * returns nothing for those events rather than everything about the event source.
     *
     * @param eventSourceId The event source identifier to get events for.
     * @param eventTypes The event types to filter for.
     * @param eventStreamType Optional event stream type to narrow to. Defaults to all stream types.
     * @param eventStreamId Optional event stream identifier to narrow to. Defaults to all streams.
     * @param eventSourceType Accepted for symmetry with appending, but **not applied**: the kernel's
     *   event-source query carries no event-source-type filter, so every source type is returned
     *   whatever is passed here. Read the stored type off [io.cratis.chronicle.events.EventContext]
     *   when it has to be distinguished.
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
     * @return A list of [AppendedEvent].
     */
    suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String? = null,
        eventTypes: List<KClass<*>>? = null
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
    suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason)

    /**
     * Redacts all events for a specific event source, optionally filtered to specific event types.
     *
     * @param eventSourceId The event source identifier to redact events for.
     * @param reason The [RedactionReason] for redacting.
     * @param eventTypes Optional event types to narrow the redaction to. If empty, all event types
     *   for the event source are redacted.
     */
    suspend fun redactForEventSource(eventSourceId: String, reason: RedactionReason, eventTypes: List<KClass<*>> = emptyList())

    /**
     * Copies the events of one event source onto another event source in this event sequence, as a
     * durable job that runs on the kernel. Copies are appended as new events with new sequence numbers;
     * the originals are left untouched.
     *
     * @param sourceId The event source identifier to copy events from.
     * @param targetId The event source identifier to copy events to.
     * @param eventTypes Optional event types to narrow the copy to. If empty, events of every type
     *   for the source event source are copied.
     * @return The identifier of the started job, usable with the jobs service to follow its progress.
     * @throws ChronicleCommandRejected if the kernel rejects the request or it is not authorized.
     */
    suspend fun copyForEventSource(sourceId: String, targetId: String, eventTypes: List<KClass<*>> = emptyList()): String

    /**
     * Appends a compensating event for an event that is already in the event sequence.
     *
     * Works like an append, except that the event source, event stream and subject are taken from the
     * event being compensated for. The original event is never changed; the new event records the
     * original's sequence number as [io.cratis.chronicle.events.EventContext.compensationFor]. The same
     * event can be compensated more than once.
     *
     * A compensation is emitted on [appendOperations] like an append, though without an event source id: the
     * kernel takes that from the original, and the emitted context carries the original's sequence number as
     * its `compensationFor`.
     *
     * A rejected compensation is reported in [AppendResult.errors] rather than thrown:
     * [AppendError.COMPENSATED_EVENT_NOT_FOUND] when there is no event at [compensationFor], and
     * [AppendError.EVENT_TYPE_DOES_NOT_COMPENSATE_EVENT_TYPE] when the type of [event] is not declared with
     * [@CompensationFor][io.cratis.chronicle.events.CompensationFor] for the type of the original.
     *
     * @param compensationFor The [EventSequenceNumber] of the event to compensate for.
     * @param event The compensating event. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
     * @param options Optional [CompensateOptions].
     * @return The [AppendResult] of the operation.
     * @throws CompensationsNotSupported if this implementation cannot append compensations.
     */
    suspend fun compensate(compensationFor: EventSequenceNumber, event: Any, options: CompensateOptions? = null): AppendResult =
        throw CompensationsNotSupported(this::class.java)

    /**
     * Imports events into the event sequence in bulk: every event is validated before any is committed,
     * and the commit runs on the kernel as a job.
     *
     * [events] is read lazily as chunks are sent, within the kernel's limits and with a bounded number of
     * chunks in flight, so a large import never holds all its events at once. Each event keeps what its
     * [EventForEventSourceId] says - when it occurred, its correlation, causation and identity, its subject
     * and tags - and gets a new sequence number. Imported events reach every observer once, as catch-up.
     *
     * ```kotlin
     * val result = eventStore.eventLog.import(legacyEvents.asFlow()) { progress -> println(progress.eventsCommitted) }
     * ```
     *
     * @param events The events, in the order they are to be committed.
     * @param options The [ImportOptions].
     * @param onProgress Receives the progress the kernel reports while the import runs.
     * @return The [ImportResult]: [ImportResult.isSuccess] when everything that was not rejected is committed.
     * @throws ImportsNotSupported if this event sequence, or the kernel, cannot import.
     */
    suspend fun import(
        events: Flow<EventForEventSourceId>,
        options: ImportOptions = ImportOptions.default,
        onProgress: FlowCollector<ImportProgress>? = null,
    ): ImportResult = throw ImportsNotSupported()

    /**
     * Imports events from a lazy sequence into the event sequence in bulk, reading it as chunks are sent.
     * Otherwise the same as the [Flow] overload.
     *
     * @param events The events, in the order they are to be committed.
     * @param options The [ImportOptions].
     * @param onProgress Receives the progress the kernel reports while the import runs.
     * @return The [ImportResult].
     * @throws ImportsNotSupported if this event sequence, or the kernel, cannot import.
     */
    suspend fun import(
        events: Sequence<EventForEventSourceId>,
        options: ImportOptions = ImportOptions.default,
        onProgress: FlowCollector<ImportProgress>? = null,
    ): ImportResult = throw ImportsNotSupported()

    /**
     * Begins an import session, for producers that are not one stream: add events to it any number of
     * times, then complete or abandon it. Keep its [IEventImport.importId] to reattach after a crash.
     *
     * @param options The [ImportOptions]; [ImportOptions.importId] begins it under a chosen identifier.
     * @return The session.
     * @throws ImportsNotSupported if this event sequence, or the kernel, cannot import.
     * @throws io.cratis.chronicle.imports.ImportCouldNotBegin if the kernel refuses to begin it.
     */
    suspend fun beginImport(options: ImportOptions = ImportOptions.default): IEventImport = throw ImportsNotSupported()

    /**
     * Reattaches to an import session that is still staging, for a producer that crashed or lost its
     * connection. Adding continues after the events the kernel acknowledged ([IEventImport.eventsAcknowledged]).
     *
     * @param importId The import to reattach to.
     * @param options The [ImportOptions]; what the import was begun with is kept.
     * @return The session.
     * @throws ImportsNotSupported if this event sequence, or the kernel, cannot import.
     * @throws io.cratis.chronicle.imports.ImportCouldNotBegin if the import does not exist or imports into another sequence.
     */
    suspend fun beginImport(importId: ImportId, options: ImportOptions = ImportOptions.default): IEventImport =
        throw ImportsNotSupported()
}
