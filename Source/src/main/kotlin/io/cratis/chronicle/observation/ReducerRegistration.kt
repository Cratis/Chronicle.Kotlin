// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Everything read off a reducer class that the kernel needs in order to register it.
 *
 * @property id The reducer identifier, defaulting to the class simple name.
 * @property eventSequenceId The event sequence to observe, defaulting to the event log.
 * @property isActive Whether the kernel should actively run the reducer.
 * @property readModelName The name of the read model the reducer produces.
 * @property readModelClass The class of the read model, inferred from the handlers' return type.
 * @property tags Descriptive labels for the reducer itself.
 * @property filters Narrows which events the kernel delivers.
 * @property handlers The per-event handler methods, keyed by event type identifier. Empty when the
 *   reducer has a [batch] method instead.
 * @property batch The batch method, when the reducer folds an event source's events in one call.
 * @property maxConcurrency How many event sources of one page are reduced at the same time.
 */
internal data class ReducerRegistration(
    val id: String,
    val eventSequenceId: String,
    val isActive: Boolean,
    val readModelName: String,
    val readModelClass: KClass<*>?,
    val tags: List<String>,
    val filters: ObserverFilters,
    val handlers: Map<String, EventHandlerMethod>,
    val batch: BatchMethod? = null,
    val maxConcurrency: Int = Runtime.getRuntime().availableProcessors()
) {
    /** Every event type the reducer folds, keyed by event type identifier. */
    val eventTypes: Map<String, KClass<*>>
        get() = batch?.eventTypes ?: handlers.mapValues { (_, handler) -> handler.eventClass }

    companion object {
        /**
         * Reads [reducerClass] by convention: any method whose first parameter is a class annotated
         * with `@EventType` is a handler, and the read model is inferred from what those methods
         * return. A method whose first parameter is an [EventBatch] is instead the reducer's one batch
         * method, folding all of an event source's events in a single call.
         */
        fun from(reducerClass: KClass<*>): ReducerRegistration {
            val annotation = reducerClass.findAnnotation<Reducer>()
            val id = annotation?.id?.ifEmpty { null } ?: reducerClass.simpleName!!

            val eventSequenceId = EventSequence.idOf(reducerClass, annotation?.eventSequence)

            val handlers = mutableMapOf<String, EventHandlerMethod>()
            var readModelClass: KClass<*>? = null

            for (function in reducerClass.memberFunctions) {
                val handler = EventHandlerMethod.from(function) ?: continue
                BatchMethods.requireNoBatchContext(reducerClass, listOf(handler))
                requireDispatchable(handler, reducerClass)
                handlers[handler.eventTypeId] = handler

                if (readModelClass == null) {
                    readModelClass = function.returnType.classifier as? KClass<*>
                }
            }

            val batch = batchMethodOf(reducerClass, hasPerEventHandlers = handlers.isNotEmpty())
            if (batch != null) readModelClass = batch.function.returnType.classifier as? KClass<*>

            if (handlers.isEmpty() && batch == null) {
                throw ObserverHasNoHandlers(
                    reducerClass,
                    "A reducer handler is a public method whose first parameter is a class annotated " +
                        "with @EventType, returning the read model it produces - or one batch method taking " +
                        "an EventBatch and the read model so far, declaring the event types it @Observes."
                )
            }

            val maxConcurrency = annotation?.maxConcurrency ?: 0
            if (maxConcurrency < 0) {
                throw IllegalArgumentException(
                    "'${reducerClass.simpleName}' sets maxConcurrency to $maxConcurrency. Use 1 or more, or 0 for the default."
                )
            }

            val readModelName = readModelClass?.let { readModel ->
                readModel.findAnnotation<ReadModel>()?.id?.ifEmpty { null } ?: readModel.simpleName
            } ?: ""

            return ReducerRegistration(
                id = id,
                eventSequenceId = eventSequenceId,
                isActive = (annotation?.isActive ?: true) && readModelClass?.findAnnotation<Passive>() == null,
                readModelName = readModelName,
                readModelClass = readModelClass,
                tags = ObserverFilters.tagsOf(reducerClass),
                filters = ObserverFilters.from(reducerClass),
                handlers = handlers,
                batch = batch,
                maxConcurrency = if (maxConcurrency == 0) Runtime.getRuntime().availableProcessors() else maxConcurrency
            )
        }

        /**
         * The reducer's batch method, if it has one. A reducer is one fold over an event source's events
         * in order, so it has either per-event handlers or exactly one batch method, never both.
         *
         * A batch method is `(batch, state)` or `(batch, state, context)`, where `state` is the read
         * model so far - or `null` - and the method returns the next one, `null` removing it.
         */
        private fun batchMethodOf(reducerClass: KClass<*>, hasPerEventHandlers: Boolean): BatchMethod? {
            val batches = BatchMethods.from(reducerClass)
            if (batches.isEmpty()) return null
            if (batches.size > 1) throw ReducerHasMoreThanOneBatchMethod(reducerClass)
            if (hasPerEventHandlers) throw ReducerMixesBatchAndPerEventMethods(reducerClass)

            val method = batches.single()
            val parameters = method.function.parameters
            val readModel = method.function.returnType.classifier as? KClass<*>

            // Index 0 is the instance receiver, so the two valid shapes arrive as 3 and 4.
            if (readModel == null || readModel == Unit::class || readModel == Any::class) {
                throw InvalidBatchMethod(reducerClass, method.name, "a reducer batch method returns the read model it produces")
            }

            if (parameters.size !in 3..4 || parameters[2].type.classifier != readModel) {
                throw InvalidBatchMethod(
                    reducerClass,
                    method.name,
                    "a reducer batch method takes the EventBatch, then the ${readModel.simpleName} so far, " +
                        "and optionally a BatchContext"
                )
            }

            if (parameters.size == 4 && !method.takesContextAt(3)) {
                throw InvalidBatchMethod(reducerClass, method.name, "its third parameter must be a BatchContext")
            }

            return method
        }

        /**
         * A reducer handler is `(event)`, `(event, state)`, or `(event, state, context)` - the same
         * shapes the C# client accepts - suspending or not.
         *
         * The mistake worth catching is `(event, context)`: it looks right next to a reactor
         * handler, but a reducer's second parameter is the state so far, so the state would be
         * handed to a parameter expecting an EventContext on every single event.
         */
        private fun requireDispatchable(handler: EventHandlerMethod, reducerClass: KClass<*>) {
            // Index 0 is the instance receiver, so the three valid shapes arrive as 2, 3, and 4.
            if (handler.parameterCount > 4) {
                handler.reject(
                    reducerClass,
                    "a reducer handler takes the event, the state so far, and optionally an EventContext"
                )
            }

            if (handler.parameterCount == 3 && handler.takesContextAt(2)) {
                handler.reject(
                    reducerClass,
                    "a reducer handler takes the state so far before the EventContext - " +
                        "add the state parameter, or drop the context"
                )
            }

            if (handler.parameterCount == 4 && !handler.takesContextAt(3)) {
                handler.reject(reducerClass, "its third parameter must be an EventContext")
            }
        }
    }
}
