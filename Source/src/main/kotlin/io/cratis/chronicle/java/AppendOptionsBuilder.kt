// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.time.Instant
import java.util.UUID

/**
 * Builds [AppendOptions] from Java.
 *
 * Kotlin callers construct [AppendOptions] directly with named arguments. Java has no equivalent, so
 * without this a Java caller would have to pass every argument positionally just to set the last
 * one. Set only what you need and call [build].
 *
 * ```java
 * var options = new AppendOptionsBuilder().subject("patient-42").tag("gdpr").build();
 * ```
 */
class AppendOptionsBuilder {
    private var correlationId: UUID? = null
    private var concurrencyScope: ConcurrencyScope? = null
    private var eventSourceType: String? = null
    private var eventStreamType: String? = null
    private var eventStreamId: String? = null
    private var subject: String? = null
    private var tags: MutableList<String> = mutableListOf()
    private var occurred: Instant? = null
    private var causation: MutableList<Causation> = mutableListOf()

    /** Sets the correlation identifier for the operation. */
    fun correlationId(correlationId: UUID): AppendOptionsBuilder = apply { this.correlationId = correlationId }

    /** Sets the [ConcurrencyScope] used for concurrency control. */
    fun concurrencyScope(concurrencyScope: ConcurrencyScope): AppendOptionsBuilder =
        apply { this.concurrencyScope = concurrencyScope }

    /** Sets the type of the event source. */
    fun eventSourceType(eventSourceType: String): AppendOptionsBuilder = apply { this.eventSourceType = eventSourceType }

    /** Sets the type of the event stream to append to. */
    fun eventStreamType(eventStreamType: String): AppendOptionsBuilder = apply { this.eventStreamType = eventStreamType }

    /** Sets the identifier of the event stream to append to. */
    fun eventStreamId(eventStreamId: String): AppendOptionsBuilder = apply { this.eventStreamId = eventStreamId }

    /** Sets the compliance subject the event is about. */
    fun subject(subject: String): AppendOptionsBuilder = apply { this.subject = subject }

    /** Adds a single tag to the event. */
    fun tag(tag: String): AppendOptionsBuilder = apply { this.tags.add(tag) }

    /** Adds all of [tags] to the event. */
    fun tags(tags: List<String>): AppendOptionsBuilder = apply { this.tags.addAll(tags) }

    /** Sets when the event actually occurred. */
    fun occurred(occurred: Instant): AppendOptionsBuilder = apply { this.occurred = occurred }

    /**
     * Adds a single [Causation] entry, overriding the ambient chain for this append.
     *
     * Leave this alone unless the append genuinely belongs to a different chain than the work the
     * current thread is doing.
     */
    fun causation(causation: Causation): AppendOptionsBuilder = apply { this.causation.add(causation) }

    /** Adds all of [causation] as the chain this append is attributed to. */
    fun causation(causation: List<Causation>): AppendOptionsBuilder = apply { this.causation.addAll(causation) }

    /** Builds the [AppendOptions]. */
    fun build(): AppendOptions = AppendOptions(
        correlationId = correlationId,
        concurrencyScope = concurrencyScope,
        eventSourceType = eventSourceType,
        eventStreamType = eventStreamType,
        eventStreamId = eventStreamId,
        subject = subject,
        tags = tags.toList(),
        occurred = occurred,
        causation = causation.toList()
    )
}
