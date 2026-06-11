// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Defines the API surface for an event sequence.
 */
interface IEventSequence {
    /** The unique identifier of this event sequence. */
    val id: EventSequenceId

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
     * Determines whether there are any events for a given event source identifier.
     *
     * @param eventSourceId The event source identifier to check.
     * @return `true` if events exist for the given source, otherwise `false`.
     */
    suspend fun hasEventsFor(eventSourceId: String): Boolean
}
