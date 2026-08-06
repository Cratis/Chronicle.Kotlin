// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder
import io.cratis.chronicle.eventSequences.operations.IEventSourceOperations
import java.time.Instant
import java.util.function.Consumer

/**
 * Java-friendly bridge for composing operations against one event source.
 *
 * [IEventSourceOperations.append] declares its shaping as optional parameters, and Kotlin default
 * arguments do not exist for Java callers of an interface method - so this repeats them as
 * `@JvmOverloads`. The scope-building overload is here for the same reason a Kotlin lambda with
 * receiver is: Java has no such thing.
 */
object EventSourceOperationsJavaBridge {
    /**
     * Stages an event to be appended to this event source.
     *
     * @param operations The event source operations to stage against.
     * @param event The event object to append.
     * @param eventStreamType Optional type of the event stream to append to.
     * @param eventStreamId Optional identifier of the event stream to append to.
     * @param eventSourceType Optional type of the event source.
     * @param tags Optional tags to attach to the event.
     * @param occurred Optional time the event actually occurred.
     * @param subject Optional compliance subject the event is about.
     * @return [operations], for chaining.
     */
    @JvmStatic
    @JvmOverloads
    fun append(
        operations: IEventSourceOperations,
        event: Any,
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null,
        tags: List<String> = emptyList(),
        occurred: Instant? = null,
        subject: String? = null
    ): IEventSourceOperations = operations.append(
        event,
        eventStreamType,
        eventStreamId,
        eventSourceType,
        tags,
        occurred,
        subject
    )

    /**
     * Builds and sets the concurrency scope this event source is checked against.
     *
     * @param operations The event source operations to configure.
     * @param configure Configures the [ConcurrencyScopeBuilder].
     * @return [operations], for chaining.
     */
    @JvmStatic
    fun withConcurrencyScope(
        operations: IEventSourceOperations,
        configure: Consumer<ConcurrencyScopeBuilder>
    ): IEventSourceOperations = operations.withConcurrencyScope { configure.accept(this) }
}
