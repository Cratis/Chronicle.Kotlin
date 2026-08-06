// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.eventSequences.IEventSequence

/**
 * Starts composing operations against this event sequence.
 *
 * Nothing is sent until [IEventSequenceOperations.perform] runs, so the composed operation can be
 * passed around and added to before it is committed.
 *
 * @return A new [EventSequenceOperations] for this event sequence.
 */
fun IEventSequence.operations(): EventSequenceOperations = EventSequenceOperations(this)

/**
 * Starts composing operations against this event sequence, configuring one event source up front.
 *
 * This is the shorthand for the common case of composing a batch that starts with a single event
 * source; chain further [IEventSequenceOperations.forEventSourceId] calls to add more.
 *
 * @param eventSourceId The identifier of the event source.
 * @param configure Configures the [IEventSourceOperations] for that event source.
 * @return A new [EventSequenceOperations] with that event source configured.
 */
fun IEventSequence.forEventSourceId(
    eventSourceId: String,
    configure: IEventSourceOperations.() -> Unit
): EventSequenceOperations = operations().forEventSourceId(eventSourceId, configure)
