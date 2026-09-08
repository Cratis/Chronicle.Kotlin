// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence

/** Defines operations composed against one sequence and performed as one explicit atomic batch. */
interface IEventSequenceOperations {
    /** The event sequence these operations target. */
    val eventSequence: IEventSequence

    /** The immutable metadata used by the entire batch. */
    val context: OperationContext

    /** Adds operations for [eventSourceId]. */
    fun forEventSourceId(eventSourceId: String, configure: IEventSourceOperations.() -> Unit): IEventSequenceOperations

    /** Returns staged payloads in append order. */
    fun getAppendedEvents(): List<Any>

    /** Returns wire-shaped staged events in append order. */
    fun getEventsToAppend(): List<EventForEventSourceId>

    /** Clears staged events while retaining the explicit operation context. */
    fun clear()

    /** Performs one atomic append-many request. */
    suspend fun perform(): List<AppendResult>
}
