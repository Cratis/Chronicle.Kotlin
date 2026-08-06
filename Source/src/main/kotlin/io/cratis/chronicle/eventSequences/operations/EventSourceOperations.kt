// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Implements [IEventSourceOperations] by accumulating operations in memory until the composed
 * operation they belong to is performed.
 */
class EventSourceOperations : IEventSourceOperations {
    private val staged = mutableListOf<IEventSequenceOperation>()

    override val operations: List<IEventSequenceOperation> get() = staged.toList()

    override var concurrencyScope: ConcurrencyScope = ConcurrencyScope.notSet
        private set

    override fun withConcurrencyScope(concurrencyScope: ConcurrencyScope): EventSourceOperations {
        // A scope already set for this event source must not be reset to notSet by a later call -
        // that would silently drop the source from the concurrency check and turn optimistic
        // concurrency off without anyone asking for it. A real scope still overrides an earlier one.
        if (concurrencyScope == ConcurrencyScope.notSet && this.concurrencyScope != ConcurrencyScope.notSet) {
            return this
        }

        this.concurrencyScope = concurrencyScope
        return this
    }

    override fun withConcurrencyScope(configure: ConcurrencyScopeBuilder.() -> Unit): EventSourceOperations =
        withConcurrencyScope(ConcurrencyScopeBuilder().apply(configure).build())

    override fun append(
        event: Any,
        eventStreamType: String?,
        eventStreamId: String?,
        eventSourceType: String?,
        tags: List<String>,
        occurred: Instant?,
        subject: String?
    ): EventSourceOperations {
        staged.add(AppendOperation(event, eventStreamType, eventStreamId, eventSourceType, tags, occurred, subject))
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : IEventSequenceOperation> getOperationsOfType(type: KClass<T>): List<T> =
        staged.filter { type.isInstance(it) } as List<T>

    override fun getAppendedEvents(): List<Any> = getOperationsOfType<AppendOperation>().map { it.event }
}
