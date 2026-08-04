// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Wraps an event together with the specific event source id it should be appended to.
 *
 * Return this (or a `List` containing it) from a [io.cratis.chronicle.observation.Reactor] handler
 * method to target an event source other than the one that triggered the reactor — the cross-stream
 * equivalent of a bare event return, which always appends to the triggering event source id.
 *
 * @property eventSourceId The event source identifier to append [event] to.
 * @property event The event object to append. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
 */
data class EventForEventSourceId(
    val eventSourceId: String,
    val event: Any
)
