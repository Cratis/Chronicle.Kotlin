// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
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
 * @property handlers The handler methods, keyed by event type identifier.
 */
internal data class ReducerRegistration(
    val id: String,
    val eventSequenceId: String,
    val isActive: Boolean,
    val readModelName: String,
    val readModelClass: KClass<*>?,
    val handlers: Map<String, EventHandlerMethod>
) {
    companion object {
        /**
         * Reads [reducerClass] by convention: any method whose first parameter is a class annotated
         * with `@EventType` is a handler, and the read model is inferred from what those methods
         * return.
         */
        fun from(reducerClass: KClass<*>): ReducerRegistration {
            val annotation = reducerClass.findAnnotation<Reducer>()
            val id = annotation?.id?.ifEmpty { null } ?: reducerClass.simpleName!!

            // An unspecified event sequence means the event log, which is where reducers observe
            // from unless they deliberately target another sequence.
            val eventSequenceId = annotation?.eventSequence?.ifEmpty { null } ?: EventSequenceId.eventLog.value

            val handlers = mutableMapOf<String, EventHandlerMethod>()
            var readModelClass: KClass<*>? = null

            for (function in reducerClass.memberFunctions) {
                val handler = EventHandlerMethod.from(function) ?: continue
                requireDispatchable(handler, reducerClass)
                handlers[handler.eventTypeId] = handler

                if (readModelClass == null) {
                    readModelClass = function.returnType.classifier as? KClass<*>
                }
            }

            val readModelName = readModelClass?.let { readModel ->
                readModel.findAnnotation<ReadModel>()?.id?.ifEmpty { null } ?: readModel.simpleName
            } ?: ""

            return ReducerRegistration(
                id = id,
                eventSequenceId = eventSequenceId,
                isActive = annotation?.isActive ?: true,
                readModelName = readModelName,
                readModelClass = readModelClass,
                handlers = handlers
            )
        }

        /**
         * A reducer handler is `(event)`, `(event, state)`, or `(event, state, context)` - the same
         * shapes the C# client accepts.
         *
         * The mistake worth catching is `(event, context)`: it looks right next to a reactor
         * handler, but a reducer's second parameter is the state so far, so the state would be
         * handed to a parameter expecting an EventContext on every single event.
         */
        private fun requireDispatchable(handler: EventHandlerMethod, reducerClass: KClass<*>) {
            handler.requireNotSuspending(reducerClass)

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
