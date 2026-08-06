// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.operations.EventSequenceOperations
import io.cratis.chronicle.eventSequences.operations.IEventSequenceOperations
import io.cratis.chronicle.eventSequences.operations.IEventSourceOperations
import io.cratis.chronicle.eventSequences.operations.operations
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking

/**
 * Java-friendly bridge for composing operations against an event sequence.
 *
 * Java cannot call the `operations()` extension function, pass a Kotlin lambda with receiver, or
 * call a `suspend` function - these three entry points cover all of it. Everything else on
 * [IEventSequenceOperations] (`withCorrelationId`, `getEventsToAppend`, `getAppendedEvents`,
 * `clear`) is a plain method Java calls directly.
 */
object EventSequenceOperationsJavaBridge {
    /**
     * Starts composing operations against [eventSequence].
     *
     * @param eventSequence The event sequence to compose operations against.
     * @return A new [EventSequenceOperations].
     */
    @JvmStatic
    fun operationsFor(eventSequence: IEventSequence): EventSequenceOperations = eventSequence.operations()

    /**
     * Composes operations for a specific event source.
     *
     * @param operations The composed operation to add to.
     * @param eventSourceId The identifier of the event source.
     * @param configure Configures the [IEventSourceOperations] for that event source.
     * @return [operations], for chaining.
     */
    @JvmStatic
    fun forEventSourceId(
        operations: IEventSequenceOperations,
        eventSourceId: String,
        configure: Consumer<IEventSourceOperations>
    ): IEventSequenceOperations = operations.forEventSourceId(eventSourceId) { configure.accept(this) }

    /**
     * Performs the composed operation, appending every staged event as one atomic batch.
     *
     * @param operations The composed operation to perform.
     * @return A list of [AppendResult], one per staged event, in append order.
     */
    @JvmStatic
    fun perform(operations: IEventSequenceOperations): List<AppendResult> = runBlocking { operations.perform() }
}
