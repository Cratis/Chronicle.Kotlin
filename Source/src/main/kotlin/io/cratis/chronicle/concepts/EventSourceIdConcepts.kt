// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("EventSourceIdConcepts")

package io.cratis.chronicle.concepts

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.readModels.IReadModelsService
import kotlin.reflect.KClass

/**
 * Everywhere the client takes an event source id as a `String`, taking a concept instead.
 *
 * The event source id is a `String` on the wire and always will be - the kernel has no notion of a
 * typed identifier. These exist so your side of the call can stay typed: pass `BookId("dune")` and
 * the compiler holds you to it, instead of `.value` appearing at every call site and taking the type
 * safety with it.
 *
 * They are extensions rather than members so that [IEventSequence] and [IReadModelsService] keep
 * exactly the surface they had - anything implementing them, a test double included, is unaffected.
 * Java reaches them as static methods on `EventSourceIdConcepts`.
 */

/** Appends [event] against the event source named by [eventSourceId]. */
suspend fun IEventSequence.append(
    eventSourceId: ConceptAs<String>,
    event: Any,
    options: AppendOptions? = null
): AppendResult = append(eventSourceId.value, event, options)

/** Appends [events] against the event source named by [eventSourceId], as one atomic batch. */
suspend fun IEventSequence.appendMany(
    eventSourceId: ConceptAs<String>,
    events: List<Any>,
    options: AppendOptions? = null
): List<AppendResult> = appendMany(eventSourceId.value, events, options)

/** Whether the sequence holds any event for [eventSourceId]. */
suspend fun IEventSequence.hasEventsFor(eventSourceId: ConceptAs<String>): Boolean =
    hasEventsFor(eventSourceId.value)

/** The position of the last event appended for [eventSourceId]. */
suspend fun IEventSequence.getTailSequenceNumber(eventSourceId: ConceptAs<String>): EventSequenceNumber =
    getTailSequenceNumber(eventSourceId.value)

/** The events of [eventTypes] appended for [eventSourceId]. */
suspend fun IEventSequence.getForEventSourceIdAndEventTypes(
    eventSourceId: ConceptAs<String>,
    eventTypes: List<KClass<*>>,
    eventStreamType: String? = null,
    eventStreamId: String? = null,
    eventSourceType: String? = null
): List<AppendedEvent> = getForEventSourceIdAndEventTypes(
    eventSourceId.value,
    eventTypes,
    eventStreamType,
    eventStreamId,
    eventSourceType
)

/** Redacts every event appended for [eventSourceId], or only those of [eventTypes]. */
suspend fun IEventSequence.redactForEventSource(
    eventSourceId: ConceptAs<String>,
    reason: RedactionReason,
    eventTypes: List<KClass<*>> = emptyList()
) = redactForEventSource(eventSourceId.value, reason, eventTypes)

/** The instance of [readModelClass] held under [key]. */
suspend fun <T : Any> IReadModelsService.getInstanceByKey(
    readModelClass: KClass<T>,
    key: ConceptAs<String>
): T? = getInstanceByKey(readModelClass, key.value)

/** An event naming the event source it belongs to with a concept rather than a bare `String`. */
fun EventForEventSourceId(eventSourceId: ConceptAs<String>, event: Any): EventForEventSourceId =
    EventForEventSourceId(eventSourceId.value, event)
