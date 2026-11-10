// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.BatchContext
import io.cratis.chronicle.observation.EventAndContext
import io.cratis.chronicle.observation.EventBatch
import io.cratis.chronicle.observation.ObservationOrigin
import io.cratis.chronicle.observation.Observes
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * The batch method of a reducer, found and called the way the client finds and calls it: the method
 * whose first parameter is an [EventBatch], fed the events its [Observes] annotations name.
 *
 * @property function The batch method.
 * @property eventClasses The event types it observes, by event type id.
 */
internal class ReducerBatchMethod private constructor(
    private val function: KFunction<*>,
    val eventClasses: Map<String, KClass<*>>,
) {
    /** The name of the batch method, for messages. */
    val name: String get() = function.name

    /**
     * Calls the batch method once with [events] - all of one event source, in order - starting from [state].
     *
     * @return The state the method returned and the batch it was called with, which holds what it reported.
     */
    suspend fun reduce(
        reducer: Any,
        observerId: String,
        eventSequenceId: String,
        events: List<EventAndContext>,
        state: Any?,
    ): Pair<Any?, EventBatch> {
        val batch = EventBatch(events)

        // Index 0 is the instance receiver, so the two shapes arrive as 3 and 4.
        val next = if (function.parameters.size == 4) {
            function.callSuspend(reducer, batch, state, contextFor(observerId, eventSequenceId, events))
        } else {
            function.callSuspend(reducer, batch, state)
        }
        return next to batch
    }

    private fun contextFor(observerId: String, eventSequenceId: String, events: List<EventAndContext>): BatchContext {
        val first: EventContext = events.first().context
        return BatchContext(
            observerId = observerId,
            eventStore = first.eventStore,
            namespace = first.namespace,
            eventSequenceId = eventSequenceId,
            origin = ObservationOrigin.Live,
            partitions = events.map { it.context.eventSourceId }.distinct(),
            eventCount = events.size,
            firstSequenceNumber = first.sequenceNumber,
            lastSequenceNumber = events.last().context.sequenceNumber,
        )
    }

    companion object {
        /**
         * Finds the batch method of [reducerClass], or `null` when it has none.
         *
         * @throws IllegalArgumentException The reducer has more than one batch method, mixes a batch method
         *   with per-event handlers, or its batch method observes nothing.
         */
        fun of(reducerClass: KClass<*>, hasPerEventHandlers: Boolean): ReducerBatchMethod? {
            val batches = reducerClass.memberFunctions.filter { it.parameters.getOrNull(1)?.type?.classifier == EventBatch::class }
            if (batches.isEmpty()) return null
            require(batches.size == 1) {
                "'${reducerClass.simpleName}' has more than one batch method; a reducer has at most one."
            }
            require(!hasPerEventHandlers) {
                "'${reducerClass.simpleName}' mixes a batch method with per-event handlers; a reducer has one or the other."
            }

            val function = batches.single()
            val observed = observesOf(function).map { it.value }
            require(observed.isNotEmpty()) {
                "The batch method '${function.name}' of '${reducerClass.simpleName}' observes nothing - " +
                    "declare each event type it takes with @Observes."
            }
            require(function.parameters.size in 3..4) {
                "The batch method '${function.name}' of '${reducerClass.simpleName}' takes the EventBatch, " +
                    "then the read model so far, and optionally a BatchContext."
            }

            // A reducer declared `internal` or private is ordinary Kotlin, and common in a spec.
            function.isAccessible = true
            val eventClasses = observed.associateBy { eventClass ->
                val annotation = requireNotNull(eventClass.findAnnotation<EventType>()) {
                    "The batch method '${function.name}' of '${reducerClass.simpleName}' observes " +
                        "'${eventClass.simpleName}', which is not annotated with @EventType."
                }
                annotation.id.ifEmpty { eventClass.simpleName!! }
            }
            return ReducerBatchMethod(function, eventClasses)
        }

        /** Kotlin reflection unwraps a repeated annotation's container; Java reflection is asked too. */
        private fun observesOf(function: KFunction<*>): List<Observes> =
            function.findAnnotations<Observes>().ifEmpty {
                function.javaMethod?.getAnnotationsByType(Observes::class.java)?.toList().orEmpty()
            }
    }
}
