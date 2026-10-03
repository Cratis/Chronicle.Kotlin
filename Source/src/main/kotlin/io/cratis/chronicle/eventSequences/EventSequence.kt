// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.Sequences
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.correlation.correlationIdManager
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.eventSources.ConcurrencyDimension
import io.cratis.chronicle.eventSources.ConflictingEventSourceConcurrency
import io.cratis.chronicle.eventSources.IEventSources
import io.cratis.chronicle.eventSources.ResolvedEventRouting
import io.cratis.chronicle.identity.Identity as ChronicleIdentity
import io.cratis.chronicle.identity.identityProvider
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.toBclGuid
import io.opentelemetry.api.common.Attributes
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Implements [IEventSequence] by communicating with the Chronicle Kernel via gRPC.
 */
open class EventSequence(
    override val id: EventSequenceId,
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
    private val traces: ChronicleTraces = ChronicleTraces.default,
    private val registrationGate: IRegistrationGate = IRegistrationGate.open,
    private val eventSources: IEventSources = IEventSources.none
) : IEventSequence, IEventSourceRoutingPreflight {
    /** The shape from before event sources existed, with its original default arguments. */
    constructor(
        id: EventSequenceId,
        eventStoreName: String,
        namespace: String,
        stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
        traces: ChronicleTraces = ChronicleTraces.default,
        registrationGate: IRegistrationGate = IRegistrationGate.open
    ) : this(id, eventStoreName, namespace, stub, traces, registrationGate, IEventSources.none)

    private val _appendOperations = MutableSharedFlow<List<AppendedEventWithResult>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val appendOperations: SharedFlow<List<AppendedEventWithResult>> = _appendOperations.asSharedFlow()

    internal var resolveConstraintMessage: (ConstraintViolation) -> ConstraintViolation = { it }

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        // The kernel rejects an event whose type it has never been told about, and registration
        // happens on connect in the background - so without this the first append after
        // getEventStore would race it. Once that pass is through, waiting costs nothing.
        registrationGate.awaitOpen()

        // Resolved once here rather than inside the span body, so naming the span costs no extra
        // reflection over what the append was going to do anyway.
        val eventType = resolveEventType(event)

        return traces.span(
            "Chronicle append ${eventType.id.value}",
            appendAttributes(eventSourceId, eventType.id.value)
        ) {
            val (routedOptions, routing) = routeThroughEventSource(eventSourceId, options)
            appendInternal(eventSourceId, event, eventType, routedOptions, routing)
        }
    }

    private suspend fun appendInternal(
        eventSourceId: String,
        event: Any,
        eventType: EventTypeDescriptor,
        options: AppendOptions?,
        routing: ResolvedEventRouting? = null
    ): AppendResult {
        val correlationId = options?.correlationId ?: correlationIdManager.current
        val concurrencyScope = options?.concurrencyScope ?: ConcurrencyScope.none
        val content = chronicleGson.toJson(event)

        val causationChain = causationFor(options?.causation ?: emptyList()) {
            causationManager.add(CausationType.appendEvent, mapOf("eventType" to eventType.id.value))
        }
        val identity = identityProvider.currentIdentity

        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.AppendRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.correlationId = correlationId.toBclGuid()
            options?.eventSourceType?.let { this.eventSourceType = it }
            this.eventSourceId = eventSourceId
            options?.eventStreamType?.let { this.eventStreamType = it }
            options?.eventStreamId?.let { this.eventStreamId = it }
            this.eventType = eventType.toContractsEventType()
            this.content = content
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
            this.subject = options.subjectOrDefault(eventSourceId)
            addAllTags(options?.tags ?: emptyList())
            options?.occurred?.let { this.occurred = it.toContractsDateTimeOffset() }
            this.concurrencyScope = concurrencyScope.toContract()
            routing?.let { this.eventSource = it.definition.name }
        }.build()

        val response = stub.append(request).ensureSuccess("append event")

        val result = mapAppendResponse(
            sequenceNumber = response.sequenceNumber,
            constraintViolations = response.constraintViolationsList,
            errors = response.errorsList,
            concurrencyViolation = if (response.hasConcurrencyViolation()) response.concurrencyViolation else null
        )

        emitAppendOperations(
            listOf(EventForEventSourceId(eventSourceId, event)),
            listOf(result),
            correlationId,
            identity,
            listOf(routing?.definition?.name.orEmpty())
        )

        return result
    }

    override suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions?
    ): List<AppendResult> {
        if (events.isEmpty()) return emptyList()

        registrationGate.awaitOpen()

        return traces.span("Chronicle appendMany", appendManyAttributes(events.size)) {
            // AppendManyRequest has no routing fields. The rich endpoint preserves supplied routes
            // without changing the ordinary batch's single-source concurrency default.
            if (options != null && (options.eventSourceType != null ||
                    options.eventStreamType != null || options.eventStreamId != null ||
                    options.eventSource != null || options.eventStream != null)) {
                appendManyForEventSourcesInternal(
                    events.map { event ->
                        EventForEventSourceId(
                            eventSourceId = eventSourceId,
                            event = event,
                            eventSourceType = options.eventSourceType,
                            eventStreamType = options.eventStreamType,
                            eventStreamId = options.eventStreamId,
                            subject = options.subject,
                            occurred = options.occurred,
                            tags = options.tags,
                            causation = options.causation,
                            eventSource = options.eventSource,
                            eventStream = options.eventStream
                        )
                    },
                    // Through a definition, leaving the scope out lets the definition's concurrency
                    // dimensions apply; an explicit scope is always forwarded and always wins.
                    if (options.eventSource != null) {
                        options.concurrencyScope?.let { mapOf(eventSourceId to it) } ?: emptyMap()
                    } else {
                        mapOf(eventSourceId to (options.concurrencyScope ?: ConcurrencyScope.none))
                    },
                    options.correlationId
                )
            } else {
                appendManyInternal(eventSourceId, events, options)
            }
        }
    }

    private suspend fun appendManyInternal(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions?
    ): List<AppendResult> {
        val correlationId = options?.correlationId ?: correlationIdManager.current
        val concurrencyScope = options?.concurrencyScope ?: ConcurrencyScope.none

        val causationChain = causationFor(options?.causation ?: emptyList()) {
            causationManager.add(CausationType.appendManyEvents, mapOf("count" to events.size.toString()))
        }
        val identity = identityProvider.currentIdentity

        val eventsForEventSourceId = events.map { event ->
            EventForEventSourceId(eventSourceId = eventSourceId, event = event, subject = options?.subject)
        }

        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.AppendManyRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
            addAllEvents(eventsForEventSourceId.map { it.toContract() })
            this.correlationId = correlationId.toBclGuid()
            addAllTags(options?.tags ?: emptyList())
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
            this.concurrencyScope = concurrencyScope.toContract()
            options?.occurred?.let { this.occurred = it.toContractsDateTimeOffset() }
        }.build()

        // A single AppendMany RPC call commits all events as one atomic operation on the kernel side,
        // rather than issuing one Append RPC per event (which would neither be atomic nor efficient).
        val response = stub.appendMany(request).ensureSuccess("append many events")

        val results = mapAppendManyResponse(events.size, response)

        emitAppendOperations(eventsForEventSourceId, results, correlationId, identity)

        return results
    }

    override suspend fun appendMany(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope>,
        correlationId: UUID?
    ): List<AppendResult> {
        if (events.isEmpty()) return emptyList()

        registrationGate.awaitOpen()

        return traces.span("Chronicle appendMany", appendManyAttributes(events.size)) {
            appendManyForEventSourcesInternal(events, concurrencyScopes, correlationId)
        }
    }

    private suspend fun appendManyForEventSourcesInternal(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope>,
        correlationId: UUID?
    ): List<AppendResult> {
        val effectiveCorrelationId = correlationId ?: correlationIdManager.current

        val routings = events.map { routingFor(it.eventSource, it.eventStream, it.eventSourceType, it.eventStreamType) }
        val routedEvents = events.mapIndexed { index, event ->
            routings[index]?.let {
                event.copy(eventSourceType = it.eventSourceType, eventStreamType = it.eventStreamType ?: event.eventStreamType)
            } ?: event
        }
        val effectiveScopes = resolveBatchScopes(routedEvents, routings, concurrencyScopes)

        val causationChain = causationFor(batchCausationOf(events)) {
            causationManager.add(CausationType.appendManyEvents, mapOf("count" to events.size.toString()))
        }
        val identity = identityProvider.currentIdentity

        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.AppendManyForEventSourcesRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            addAllEvents(routedEvents.mapIndexed { index, event -> event.toContractForEventSource(routings[index]?.definition?.name) })
            this.correlationId = effectiveCorrelationId.toBclGuid()
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
            addAllConcurrencyScopes(
                effectiveScopes.map { (source, scope) ->
                    Sequences.EventSourceConcurrencyScope.newBuilder()
                        .setEventSourceId(source)
                        .setScope(scope.toContract())
                        .build()
                }
            )
        }.build()

        // A single AppendManyForEventSources RPC call commits all events as one atomic operation on
        // the kernel side, rather than issuing one Append RPC per event source.
        val response = stub.appendManyForEventSources(request).ensureSuccess("append many events for event sources")

        val results = mapAppendManyResponse(events.size, response)

        emitAppendOperations(
            events,
            results,
            effectiveCorrelationId,
            identity,
            routings.map { it?.definition?.name.orEmpty() }
        )

        return results
    }

    override suspend fun hasEventsFor(eventSourceId: String): Boolean {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.HasEventsForEventSourceIdRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
        }.build()

        val response = stub.hasEventsForEventSourceId(request).ensureSuccess("has events for event source")
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
        val request = Sequences.ForEventSourceIdAndEventTypesRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
            eventStreamType?.let { this.eventStreamType = it }
            eventStreamId?.let { this.eventStreamId = it }
            eventSourceType?.let { this.eventSourceType = it }
            this.eventTypeIds = joinEventTypeIds(eventTypes)
        }.build()

        val response = stub.forEventSourceIdAndEventTypes(request).ensureSuccess("for event source id and event types")
        return response.dataList.map { it.toClient(esName, ns) }
    }

    override suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String?,
        eventTypes: List<KClass<*>>?
    ): List<AppendedEvent> {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.FromSequenceNumberRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.fromEventSequenceNumber = sequenceNumber.value
            eventSourceId?.let { this.eventSourceId = it }
            eventTypes?.let { this.eventTypeIds = joinEventTypeIds(it) }
        }.build()

        val response = stub.fromSequenceNumber(request).ensureSuccess("from sequence number")
        return response.dataList.map { it.toClient(esName, ns) }
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
        val request = Sequences.CompleteStreamRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventStreamType = eventStreamType
            this.eventStreamId = eventStreamId
        }.build()

        val response = stub.completeStream(request).ensureSuccess("complete stream")
        if (response.isSuccess) {
            return CompleteStreamResult.Success(EventSequenceNumber(response.sequenceNumber))
        }

        return when (response.error) {
            Sequences.CompleteStreamError.DefaultStreamCannotBeCompleted -> CompleteStreamResult.DefaultStreamCannotBeCompleted
            else -> CompleteStreamResult.AlreadyCompleted
        }
    }

    override suspend fun redact(sequenceNumber: EventSequenceNumber, reason: RedactionReason) {
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.RedactRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.sequenceNumber = sequenceNumber.value
            this.reason = reason.value
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
        }.build()

        stub.redact(request).ensureSuccess("redact event")
    }

    override suspend fun redactForEventSource(eventSourceId: String, reason: RedactionReason, eventTypes: List<KClass<*>>) {
        val causationChain = causationManager.currentChain
        val identity = identityProvider.currentIdentity
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.RedactForEventSourceRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            this.eventSourceId = eventSourceId
            this.reason = reason.value
            addAllEventTypes(eventTypes.map { resolveEventTypeFor(it).id.value })
            addAllCausation(causationChain.map { c -> c.toContractsCausation() })
            this.causedBy = identity.withoutDuplicates().toContractsIdentity()
        }.build()

        stub.redactForEventSource(request).ensureSuccess("redact event source")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Publishes to [appendOperations] after a completed append through this instance, whether it
     * succeeded or failed. The occurred time is approximated client-side as the server does not echo
     * it back on [Sequences.AppendResponse]/[Sequences.AppendManyResponse].
     */
    // -------------------------------------------------------------------------
    // Event source definitions
    // -------------------------------------------------------------------------

    override fun preflightRouting(options: AppendOptions?) {
        routingFor(options?.eventSource, options?.eventStream, options?.eventSourceType, options?.eventStreamType)
    }

    private fun routingFor(
        eventSource: KClass<*>?,
        eventStream: String?,
        eventSourceType: String?,
        eventStreamType: String?
    ): ResolvedEventRouting? {
        if (eventSource == null) {
            require(eventStream == null) { "An event stream '$eventStream' can only be named together with an event source." }
            return null
        }
        return ResolvedEventRouting.resolve(eventSources, eventSource, eventStream, eventSourceType, eventStreamType)
    }

    /**
     * Resolves a definition-aware append: stamps the event source type and stream from the definition and,
     * unless the caller supplied a concurrency scope, applies the definition's concurrency dimensions.
     */
    private suspend fun routeThroughEventSource(
        eventSourceId: String,
        options: AppendOptions?
    ): Pair<AppendOptions?, ResolvedEventRouting?> {
        val routing = routingFor(options?.eventSource, options?.eventStream, options?.eventSourceType, options?.eventStreamType)
            ?: return options to null
        val requested = checkNotNull(options)

        val routed = requested.copy(
            eventSourceType = routing.eventSourceType,
            eventStreamType = routing.eventStreamType ?: requested.eventStreamType
        )
        val explicit = requested.concurrencyScope
        if (explicit != null && explicit != ConcurrencyScope.notSet) return routed to routing

        val scope = scopeFor(routing, eventSourceId, routed.eventStreamType, routed.eventStreamId, routed.eventSourceType)
        return (if (scope != null) routed.copy(concurrencyScope = scope) else routed) to routing
    }

    /**
     * Resolves the batch's concurrency scopes. The wire carries one scope per event source id, so a scope is
     * derived per entry and entries sharing an id must agree: a derived scope is never taken from the first
     * entry alone. Explicit per-id scopes keep precedence over anything derived.
     */
    private suspend fun resolveBatchScopes(
        events: List<EventForEventSourceId>,
        routings: List<ResolvedEventRouting?>,
        concurrencyScopes: Map<String, ConcurrencyScope>
    ): Map<String, ConcurrencyScope> {
        val resolved = concurrencyScopes.toMutableMap()
        val derivedCache = mutableMapOf<ScopeKey, ConcurrencyScope?>()
        val routedBySource = events.indices.filter { routings[it] != null }.groupBy { events[it].eventSourceId }

        for ((eventSourceId, indices) in routedBySource) {
            val existing = resolved[eventSourceId]
            if (existing != null && existing != ConcurrencyScope.notSet) continue

            val derived = indices.map { index ->
                val event = events[index]
                val routing = routings[index]!!
                val key = ScopeKey(routing.dimensions, eventSourceId, event.eventStreamType, event.eventStreamId, event.eventSourceType)
                if (key in derivedCache) {
                    derivedCache[key]
                } else {
                    scopeFor(routing, eventSourceId, event.eventStreamType, event.eventStreamId, event.eventSourceType)
                        .also { derivedCache[key] = it }
                }
            }
            if (derived.distinct().size > 1) throw ConflictingEventSourceConcurrency(eventSourceId)
            derived.first()?.let { resolved[eventSourceId] = it }
        }
        return resolved
    }

    private data class ScopeKey(
        val dimensions: Set<ConcurrencyDimension>,
        val eventSourceId: String,
        val eventStreamType: String?,
        val eventStreamId: String?,
        val eventSourceType: String?
    )

    /**
     * Builds the concurrency scope for the dimensions a definition declares, or `null` when it declares none
     * (or none of the declared dimensions has a value), which leaves the append unchecked as it is without a
     * definition. A dimension without a known value is not scoped on.
     */
    private suspend fun scopeFor(
        routing: ResolvedEventRouting,
        eventSourceId: String,
        eventStreamType: String?,
        eventStreamId: String?,
        eventSourceType: String?
    ): ConcurrencyScope? {
        val dimensions = routing.dimensions
        val scopedSourceId = ConcurrencyDimension.EventSourceId in dimensions
        val scopedSourceType = eventSourceType?.takeIf { ConcurrencyDimension.EventSourceType in dimensions && it.isNotEmpty() }
        val scopedStreamType = eventStreamType?.takeIf { ConcurrencyDimension.EventStreamType in dimensions && it.isNotEmpty() }
        val scopedStreamId = eventStreamId?.takeIf { ConcurrencyDimension.EventStreamId in dimensions && it.isNotEmpty() }

        if (!scopedSourceId && scopedSourceType == null && scopedStreamType == null && scopedStreamId == null) return null

        val tail = getTailSequenceNumberInternal(
            eventSourceId = eventSourceId.takeIf { scopedSourceId },
            filterEventTypes = emptyList(),
            eventSourceType = scopedSourceType,
            eventStreamType = scopedStreamType,
            eventStreamId = scopedStreamId
        )

        // Nothing matches yet: the scope expects it to stay that way until this append lands.
        return ConcurrencyScope(
            sequenceNumber = if (tail.isActualValue) tail else EventSequenceNumber.unavailable,
            eventSourceId = scopedSourceId,
            eventStreamType = scopedStreamType,
            eventStreamId = scopedStreamId,
            eventSourceType = scopedSourceType,
            expectsNoMatchingEvent = !tail.isActualValue
        )
    }

    private fun emitAppendOperations(
        events: List<EventForEventSourceId>,
        results: List<AppendResult>,
        correlationId: UUID,
        causedBy: ChronicleIdentity,
        eventSourceNames: List<String> = emptyList()
    ) {
        val occurred = Instant.now()
        val entries = events.mapIndexed { index, event ->
            val context = io.cratis.chronicle.events.EventContext(
                sequenceNumber = results[index].sequenceNumber.value,
                eventSourceId = event.eventSourceId,
                eventType = resolveEventType(event.event),
                occurred = occurred,
                correlationId = correlationId,
                causedBy = causedBy,
                eventSource = eventSourceNames.getOrElse(index) { "" }
            )
            AppendedEventWithResult(context, event.event, results[index])
        }
        _appendOperations.tryEmit(entries)
    }

    /** What a reader of a trace needs to find this append: which event, where it went. */
    private fun appendAttributes(eventSourceId: String, eventTypeId: String): Attributes = Attributes.of(
        ChronicleTraces.EVENT_TYPE, eventTypeId,
        ChronicleTraces.EVENT_SOURCE_ID, eventSourceId,
        ChronicleTraces.EVENT_SEQUENCE_ID, id.value,
        ChronicleTraces.EVENT_STORE, eventStoreName,
        ChronicleTraces.NAMESPACE, namespace
    )

    /**
     * The same for a batch, minus the event type and source - a batch may span many of both, and the
     * count is what tells you whether you are looking at the batch you meant to.
     */
    private fun appendManyAttributes(eventCount: Int): Attributes = Attributes.builder()
        .put(ChronicleTraces.EVENT_SEQUENCE_ID, id.value)
        .put(ChronicleTraces.EVENT_STORE, eventStoreName)
        .put(ChronicleTraces.NAMESPACE, namespace)
        .put(ChronicleTraces.EVENT_COUNT, eventCount.toLong())
        .build()

    /**
     * The causation chain to send: [override] when the caller supplied one, otherwise the ambient
     * chain for this thread after [recordAmbient] has noted the append on it.
     *
     * An override deliberately leaves the ambient chain alone. The point of overriding is that this
     * append is not part of the work the current thread is doing, so recording it there would
     * attribute later appends to something they had nothing to do with.
     */
    private fun causationFor(override: List<Causation>, recordAmbient: () -> Unit): List<Causation> {
        if (override.isNotEmpty()) return override
        recordAmbient()
        return causationManager.currentChain
    }

    /**
     * The one causation chain a batch is appended under.
     *
     * The kernel's AppendMany request carries a single chain for the whole batch rather than one per
     * event, so events that disagree cannot be expressed. Rejecting that says so plainly, where
     * quietly picking one event's chain would attribute the rest of the batch to a cause that is not
     * theirs.
     */
    private fun batchCausationOf(events: List<EventForEventSourceId>): List<Causation> {
        val declared = events.map { it.causation }.filter { it.isNotEmpty() }.distinct()

        if (declared.size > 1) {
            throw CausationDiffersAcrossBatch(events.size)
        }

        return declared.singleOrNull() ?: emptyList()
    }

    /**
     * The minimal shape carried inside a single-event-source [Sequences.AppendManyRequest] batch,
     * where the event source is shared at the request level. This endpoint has no routing fields;
     * batches with routing options use the rich endpoint instead.
     */
    private fun EventForEventSourceId.toContract(): Sequences.EventToAppend =
        Sequences.EventToAppend.newBuilder()
            .setEventType(resolveEventType(this.event).toContractsEventType())
            .setContent(chronicleGson.toJson(this.event))
            .setSubject(this.subject ?: this.eventSourceId)
            .build()

    /**
     * The rich shape carried inside an [Sequences.AppendManyForEventSourcesRequest] batch, where each
     * event names its own event source and stream since the batch can span many of both.
     *
     * Routing is sent only when supplied; the kernel owns missing/empty routing defaults.
     * Subject fallback remains independent of routing.
     */
    private fun EventForEventSourceId.toContractForEventSource(eventSourceName: String? = null): Sequences.EventForEventSourceId =
        Sequences.EventForEventSourceId.newBuilder().apply {
            eventSourceName?.let { this.eventSource = it }
            this@toContractForEventSource.eventSourceType?.let { this.eventSourceType = it }
            this.eventSourceId = this@toContractForEventSource.eventSourceId
            this@toContractForEventSource.eventStreamType?.let { this.eventStreamType = it }
            this@toContractForEventSource.eventStreamId?.let { this.eventStreamId = it }
            this.eventType = resolveEventType(this@toContractForEventSource.event).toContractsEventType()
            this.content = chronicleGson.toJson(this@toContractForEventSource.event)
            this.subject = this@toContractForEventSource.subject ?: this@toContractForEventSource.eventSourceId
            addAllTags(this@toContractForEventSource.tags)
            this@toContractForEventSource.occurred?.let { this.occurred = it.toContractsDateTimeOffset() }
        }.build()

    private suspend fun getTailSequenceNumberInternal(
        eventSourceId: String?,
        filterEventTypes: List<EventTypeDescriptor>,
        eventSourceType: String? = null,
        eventStreamType: String? = null,
        eventStreamId: String? = null
    ): EventSequenceNumber {
        val esName = eventStoreName
        val ns = this@EventSequence.namespace
        val request = Sequences.TailSequenceNumberRequest.newBuilder().apply {
            this.eventStore = esName
            this.namespace = ns
            this.eventSequenceId = id.value
            eventSourceId?.let { this.eventSourceId = it }
            eventSourceType?.let { this.eventSourceType = it }
            eventStreamType?.let { this.eventStreamType = it }
            eventStreamId?.let { this.eventStreamId = it }
            this.eventTypeIds = filterEventTypes.joinToString(",") { it.id.value }
        }.build()

        val response = stub.tailSequenceNumber(request).ensureSuccess("get tail sequence number")
        return EventSequenceNumber(response.sequenceNumber)
    }

    /** The kernel takes filter event types as a single comma-joined id string, not a repeated field. */
    private fun joinEventTypeIds(eventTypes: List<KClass<*>>): String =
        eventTypes.joinToString(",") { resolveEventTypeFor(it).id.value }

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
        constraintViolations: List<Sequences.ConstraintViolation>,
        errors: List<String>,
        concurrencyViolation: Sequences.ConcurrencyViolation?
    ): AppendResult {
        val mappedViolations = constraintViolations.map { resolveConstraintMessage(it.toClient()) }
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

    private fun mapAppendManyResponse(eventCount: Int, response: Sequences.AppendManyResponse): List<AppendResult> {
        val mappedViolations = response.constraintViolationsList.map { resolveConstraintMessage(it.toClient()) }
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

/**
 * The compliance subject defaults to the event source, matching the .NET client - the event is
 * about the thing it happened to unless the caller says otherwise.
 */
private fun AppendOptions?.subjectOrDefault(eventSourceId: String): String =
    this?.subject ?: eventSourceId

private fun Instant.toContractsDateTimeOffset(): Sequences.SerializableDateTimeOffset =
    Sequences.SerializableDateTimeOffset.newBuilder()
        .setValue(DateTimeFormatter.ISO_INSTANT.format(this))
        .build()

private fun EventTypeDescriptor.toContractsEventType(): Sequences.EventType =
    Sequences.EventType.newBuilder()
        .setId(id.value)
        .setGeneration(generation.value)
        .setTombstone(tombstone)
        .build()

private fun Sequences.ConstraintViolation.toClient(): ConstraintViolation = ConstraintViolation(
    constraintId = constraintName,
    message = message,
    details = detailsMap.toMap()
)

private fun Sequences.ConcurrencyViolation.toClient(): io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation =
    io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation(
        eventSourceId = eventSourceId,
        expectedSequenceNumber = EventSequenceNumber(expectedSequenceNumber),
        actualSequenceNumber = EventSequenceNumber(actualSequenceNumber)
    )

private fun ConcurrencyScope.toContract(): Sequences.ConcurrencyScope {
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

private fun io.cratis.chronicle.auditing.Causation.toContractsCausation(): Sequences.Causation =
    Sequences.Causation.newBuilder()
        .setOccurred(
            Sequences.SerializableDateTimeOffset.newBuilder()
                .setValue(DateTimeFormatter.ISO_INSTANT.format(timestamp))
                .build()
        )
        .setType(type.name)
        .putAllProperties(properties)
        .build()

private fun ChronicleIdentity.toContractsIdentity(): Sequences.Identity {
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

private fun Sequences.CommandResult.ensureSuccess(operation: String) =
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)

private fun Sequences.CommandResult_AppendResponse.ensureSuccess(operation: String): Sequences.AppendResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

private fun Sequences.CommandResult_AppendManyResponse.ensureSuccess(operation: String): Sequences.AppendManyResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

private fun Sequences.CommandResult_CompleteStreamResponse.ensureSuccess(operation: String): Sequences.CompleteStreamResponse {
    ensureSuccessMessage(operation, isAuthorized, authorizationFailureReason, exceptionMessagesList)
    return response
}

private fun Sequences.QueryResult_EventSourceEventsResponse.ensureSuccess(operation: String): Sequences.EventSourceEventsResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return data
}

private fun Sequences.QueryResult_EventSequenceTailResponse.ensureSuccess(operation: String): Sequences.EventSequenceTailResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return data
}

private fun Sequences.QueryResult_IEnumerable_AppendedEventResponse.ensureSuccess(operation: String): Sequences.QueryResult_IEnumerable_AppendedEventResponse {
    ensureSuccessMessage(operation, isAuthorized, "", exceptionMessagesList)
    return this
}
