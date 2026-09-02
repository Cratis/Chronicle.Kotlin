// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.IEventSequence

/** Starts composing a batch with a fresh per-call system context. */
fun IEventSequence.operations(): EventSequenceOperations = operations(OperationContext.system())

/** Starts composing a batch with explicit immutable metadata. */
fun IEventSequence.operations(context: OperationContext): EventSequenceOperations =
    EventSequenceOperations(this, context)

/** Starts composing a batch with one event source and a fresh per-call system context. */
fun IEventSequence.forEventSourceId(
    eventSourceId: String,
    configure: IEventSourceOperations.() -> Unit
): EventSequenceOperations = operations().forEventSourceId(eventSourceId, configure)

/** Starts composing a batch with one event source and explicit immutable metadata. */
fun IEventSequence.forEventSourceId(
    eventSourceId: String,
    context: OperationContext,
    configure: IEventSourceOperations.() -> Unit
): EventSequenceOperations = operations(context).forEventSourceId(eventSourceId, configure)
