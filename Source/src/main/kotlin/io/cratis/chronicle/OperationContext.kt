// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.identity.Identity
import java.time.Instant
import java.util.Collections
import java.util.UUID

/**
 * Immutable metadata carried explicitly by an event operation.
 *
 * An operation context is a value, not ambient state. It is therefore safe to pass across dispatcher
 * hops and between parallel coroutines. Use [system] for work initiated by the application itself,
 * or construct a context from an existing event's correlation, causation, and identity metadata.
 *
 * @property correlationId Identifier shared by related operations.
 * @property causation The immutable chain describing what caused the operation.
 * @property causedBy The identity responsible for the operation.
 */
class OperationContext @JvmOverloads constructor(
    val correlationId: UUID,
    causation: List<Causation> = emptyList(),
    val causedBy: Identity = Identity.system
) {
    /** A defensive, immutable copy of the causation chain. */
    val causation: List<Causation> = immutableCausation(causation)

    /** Copies this context, replacing only the supplied values. */
    @JvmOverloads
    fun copy(
        correlationId: UUID = this.correlationId,
        causation: List<Causation> = this.causation,
        causedBy: Identity = this.causedBy
    ): OperationContext = OperationContext(correlationId, causation, causedBy)

    /** Returns a copy with [causation] appended to the chain. */
    fun causedBy(causation: Causation): OperationContext = copy(causation = this.causation + causation)

    override fun equals(other: Any?): Boolean =
        other is OperationContext &&
            correlationId == other.correlationId &&
            causation == other.causation &&
            causedBy == other.causedBy

    override fun hashCode(): Int {
        var result = correlationId.hashCode()
        result = 31 * result + causation.hashCode()
        result = 31 * result + causedBy.hashCode()
        return result
    }

    override fun toString(): String =
        "OperationContext(correlationId=$correlationId, causation=$causation, causedBy=$causedBy)"

    companion object {
        /** Creates a fresh context for one system-initiated call. */
        @JvmStatic
        fun system(): OperationContext = OperationContext(
            UUID.randomUUID(),
            listOf(Causation(Instant.now(), CausationType.root)),
            Identity.system
        )

        /** Creates a context with explicit metadata. */
        @JvmStatic
        @JvmOverloads
        fun of(
            correlationId: UUID,
            causedBy: Identity,
            causation: List<Causation> = emptyList()
        ): OperationContext = OperationContext(correlationId, causation, causedBy)

        /** Starts a Java-friendly builder. */
        @JvmStatic
        fun builder(): Builder = Builder()

        /** Starts a Java-friendly builder initialized from [context]. */
        @JvmStatic
        fun builder(context: OperationContext): Builder = Builder(context)

        private fun immutableCausation(causation: List<Causation>): List<Causation> =
            Collections.unmodifiableList(
                causation.map { entry ->
                    entry.copy(properties = Collections.unmodifiableMap(LinkedHashMap(entry.properties)))
                }
            )
    }

    /** Java-friendly builder for [OperationContext]. */
    class Builder internal constructor(context: OperationContext? = null) {
        private var correlationId: UUID = context?.correlationId ?: UUID.randomUUID()
        private var causedBy: Identity = context?.causedBy ?: Identity.system
        private val causation = context?.causation?.toMutableList() ?: mutableListOf()

        /** Sets the correlation identifier. */
        fun correlationId(correlationId: UUID): Builder = apply { this.correlationId = correlationId }

        /** Sets the identity responsible for the operation. */
        fun causedBy(causedBy: Identity): Builder = apply { this.causedBy = causedBy }

        /** Adds one causation entry. */
        fun causation(causation: Causation): Builder = apply { this.causation.add(causation) }

        /** Replaces the causation chain. */
        fun causation(causation: List<Causation>): Builder = apply {
            this.causation.clear()
            this.causation.addAll(causation)
        }

        /** Builds an immutable context. */
        fun build(): OperationContext = OperationContext(correlationId, causation, causedBy)
    }
}
