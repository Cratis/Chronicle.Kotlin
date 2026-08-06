// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Defines the operations composed against one event source within an event sequence.
 *
 * Everything staged here shares an event source id, which is why the concurrency scope lives at
 * this level: optimistic concurrency is checked per event source, not per event.
 */
interface IEventSourceOperations {
    /** The operations staged against this event source, in the order they were added. */
    val operations: List<IEventSequenceOperation>

    /** The [ConcurrencyScope] this event source is checked against. [ConcurrencyScope.notSet] means unchecked. */
    val concurrencyScope: ConcurrencyScope

    /**
     * Sets the [ConcurrencyScope] this event source is checked against.
     *
     * A [ConcurrencyScope.notSet] value does not override a scope that has already been set, so a
     * later call that expresses no expectation cannot silently disable the concurrency check. Any
     * other value overrides whatever was set before.
     *
     * @param concurrencyScope The concurrency scope to set.
     * @return This instance, for chaining.
     */
    fun withConcurrencyScope(concurrencyScope: ConcurrencyScope): IEventSourceOperations

    /**
     * Builds and sets the [ConcurrencyScope] this event source is checked against.
     *
     * @param configure Configures the [ConcurrencyScopeBuilder].
     * @return This instance, for chaining.
     */
    fun withConcurrencyScope(configure: ConcurrencyScopeBuilder.() -> Unit): IEventSourceOperations

    /**
     * Stages an event to be appended to this event source.
     *
     * Nothing is sent to the kernel until [IEventSequenceOperations.perform] runs.
     *
     * @param event The event object to append. Must be annotated with [@EventType][io.cratis.chronicle.events.EventType].
     * @param eventStreamType Optional type of the event stream to append to. Defaults to `Default`.
     * @param eventStreamId Optional identifier of the event stream to append to. Defaults to the event source id.
     * @param eventSourceType Optional type of the event source. Defaults to `Default`.
     * @param tags Optional tags to attach to the event.
     * @param occurred Optional time the event actually occurred. Defaults to the time the kernel appends it.
     * @param subject Optional compliance subject the event is about. Defaults to the event source id.
     * @return This instance, for chaining.
     */
    fun append(
        event: Any,
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null,
        tags: List<String> = emptyList(),
        occurred: Instant? = null,
        subject: String? = null
    ): IEventSourceOperations

    /**
     * Gets the staged operations of a specific type.
     *
     * @param type The operation type to filter by.
     * @return The matching operations, in the order they were added.
     */
    fun <T : IEventSequenceOperation> getOperationsOfType(type: KClass<T>): List<T>

    /**
     * Gets the events staged for this event source.
     *
     * @return The staged events, in the order they were added.
     */
    fun getAppendedEvents(): List<Any>
}

/**
 * Gets the staged operations of the reified type [T].
 *
 * @return The matching operations, in the order they were added.
 */
inline fun <reified T : IEventSequenceOperation> IEventSourceOperations.getOperationsOfType(): List<T> =
    getOperationsOfType(T::class)
