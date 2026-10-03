// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("EventSequenceEventSources")

package io.cratis.chronicle.eventSequences

import kotlin.reflect.KClass

/**
 * Appends an event through a registered event source definition.
 *
 * The event source type is taken from the definition, [eventStream] is validated against the streams it
 * declares, the event source name is recorded on the event, and the definition's concurrency dimensions
 * apply unless [options] carries an explicit concurrency scope. Explicit routing in [options] that
 * contradicts the definition is rejected.
 *
 * @param eventSource The class carrying the [io.cratis.chronicle.eventSources.EventSource] definition.
 * @param eventSourceId The identifier of the event source instance.
 * @param event The event to append.
 * @param eventStream The name of a stream the definition declares, if any.
 * @param options Further [AppendOptions]; its own event source routing is replaced by the arguments.
 * @return The [AppendResult] of the operation.
 * @throws io.cratis.chronicle.eventSources.UnknownEventSource The class is not a discovered definition.
 * @throws io.cratis.chronicle.eventSources.EventStreamDoesNotBelongToEventSource The stream is not declared.
 * @throws io.cratis.chronicle.eventSources.EventRoutingContradictsEventSource Explicit routing contradicts the definition.
 */
@JvmOverloads
suspend fun IEventSequence.appendThroughEventSource(
    eventSource: KClass<*>,
    eventSourceId: String,
    event: Any,
    eventStream: String? = null,
    options: AppendOptions = AppendOptions()
): AppendResult = append(eventSourceId, event, options.copy(eventSource = eventSource, eventStream = eventStream))

/**
 * Appends several events for one event source as one atomic batch, through a registered event source
 * definition. See [appendThroughEventSource] for how the definition is applied.
 *
 * @param eventSource The class carrying the [io.cratis.chronicle.eventSources.EventSource] definition.
 * @param eventSourceId The identifier of the event source instance.
 * @param events The events to append.
 * @param eventStream The name of a stream the definition declares, if any.
 * @param options Further [AppendOptions]; its own event source routing is replaced by the arguments.
 * @return A list of [AppendResult], one per event.
 */
@JvmOverloads
suspend fun IEventSequence.appendManyThroughEventSource(
    eventSource: KClass<*>,
    eventSourceId: String,
    events: List<Any>,
    eventStream: String? = null,
    options: AppendOptions = AppendOptions()
): List<AppendResult> = appendMany(eventSourceId, events, options.copy(eventSource = eventSource, eventStream = eventStream))
