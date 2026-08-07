// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence
import java.util.UUID

/**
 * Defines a set of operations composed against an event sequence and performed as one atomic batch.
 *
 * A direct `append`/`appendMany` decides everything at the call site, which stops working as soon as
 * the events for one unit of work are decided in more than one place, or target more than one event
 * source. Compose them here instead: stage events per event source, inspect what is about to be
 * sent, then [perform] once.
 *
 * Causation is not composed here - the Kotlin client derives it from the ambient
 * [io.cratis.chronicle.auditing.CausationManager] at the moment [perform] runs.
 */
interface IEventSequenceOperations {
    /** The event sequence these operations are composed against. */
    val eventSequence: IEventSequence

    /**
     * Composes operations for a specific event source.
     *
     * Calling this more than once for the same event source adds to what is already staged rather
     * than replacing it, so one composed operation can be built up across several call sites.
     *
     * @param eventSourceId The identifier of the event source.
     * @param configure Configures the [IEventSourceOperations] for that event source.
     * @return This instance, for chaining.
     */
    fun forEventSourceId(eventSourceId: String, configure: IEventSourceOperations.() -> Unit): IEventSequenceOperations

    /**
     * Sets the correlation identifier for the composed operation.
     *
     * Correlation identifies the whole unit of work, so it belongs here rather than on an individual
     * event. Without it the current [io.cratis.chronicle.correlation.CorrelationIdManager] value is used.
     *
     * @param correlationId The correlation identifier to use.
     * @return This instance, for chaining.
     */
    fun withCorrelationId(correlationId: UUID): IEventSequenceOperations

    /**
     * Attributes the composed operation to [causation] rather than the ambient chain.
     *
     * Like correlation, causation describes the whole batch: the kernel carries one chain per
     * append, not one per event, so this belongs here rather than on an individual staged event.
     * Without it the chain the current thread has built up is used, which is what nearly every
     * caller wants.
     *
     * @param causation The chain this batch should be attributed to.
     * @return This instance, for chaining.
     */
    fun withCausation(causation: List<Causation>): IEventSequenceOperations

    /**
     * Gets the events staged across every event source.
     *
     * @return The staged events, in the order they will be appended.
     */
    fun getAppendedEvents(): List<Any>

    /**
     * Gets what will go on the wire, in the order it will be sent.
     *
     * @return The staged [EventForEventSourceId] records, in append order.
     */
    fun getEventsToAppend(): List<EventForEventSourceId>

    /** Clears everything composed so far, leaving the instance reusable. */
    fun clear()

    /**
     * Performs the composed operation, appending every staged event as one atomic batch.
     *
     * @return A list of [AppendResult], one per staged event, in append order.
     */
    suspend fun perform(): List<AppendResult>
}
