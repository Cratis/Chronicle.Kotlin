// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import bcl.Bcl
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

private val gson = Gson()

class ReducersService(
    private val eventStoreName: String,
    private val namespace: String,
    private val connectionId: String,
    private val stub: ReducersGrpcKt.ReducersCoroutineStub
) : IReducersService {

    override suspend fun register(reducer: Any): Job {
        val reducerClass = reducer::class
        val annotation = reducerClass.findAnnotation<Reducer>()
        val reducerId = annotation?.id?.ifEmpty { reducerClass.simpleName!! }
            ?: reducerClass.simpleName!!

        // Find the read model type from Reducer or by inspecting return types
        var readModelClass: KClass<*>? = null
        val handlersByEventTypeId = mutableMapOf<String, Pair<kotlin.reflect.KFunction<*>, KClass<*>>>()

        for (fn in reducerClass.memberFunctions) {
            val params = fn.parameters
            if (params.size < 2) continue
            val eventParam = params[1]
            val eventKClass = eventParam.type.classifier as? KClass<*> ?: continue
            val eventAnnotation = eventKClass.findAnnotation<EventType>() ?: continue
            val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }
            handlersByEventTypeId[eventTypeId] = fn to eventKClass
            // Infer read model from return type
            if (readModelClass == null) {
                readModelClass = fn.returnType.classifier as? KClass<*>
            }
        }

        val readModelAnn = readModelClass?.findAnnotation<ReadModel>()
        val readModelName = readModelAnn?.id?.ifEmpty { readModelClass?.simpleName ?: "" }
            ?: readModelClass?.simpleName ?: ""

        val eventTypes = handlersByEventTypeId.map { (id, pair) ->
            val (_, eventKClass) = pair
            val ann = eventKClass.findAnnotation<EventType>()!!
            ObservationReducers.EventTypeWithKeyExpression.newBuilder()
                .setEventType(
                    ObservationReducers.EventType.newBuilder()
                        .setId(id)
                        .setGeneration(ann.generation)
                        .build()
                )
                .setKey("EventSourceId")
                .build()
        }

        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReducers.ReducerMessage>(Channel.BUFFERED)

        return CoroutineScope(Dispatchers.IO).launch {
            requests.send(
                ObservationReducers.ReducerMessage.newBuilder()
                    .setContent(
                        ObservationReducers.OneOf_RegisterReducer_ReducerResult.newBuilder()
                            .setValue0(
                                ObservationReducers.RegisterReducer.newBuilder()
                                    .setConnectionId(connectionId)
                                    .setEventStore(eventStoreName)
                                    .setNamespace(namespace)
                                    .setReducer(
                                        ObservationReducers.ReducerDefinition.newBuilder()
                                            .setReducerId(reducerId)
                                            .setEventSequenceId(EventSequenceId.eventLog.value)
                                            .setReadModel(readModelName)
                                            .setIsActive(true)
                                            .addAllEventTypes(eventTypes)
                                            .setSink(
                                                ObservationReducers.SinkDefinition.newBuilder()
                                                    .setConfigurationId(
                                                        Bcl.Guid.newBuilder().setLo(1L).setHi(0L).build()
                                                    )
                                                    .setTypeId("MongoDB")
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

            stub.observe(requests.receiveAsFlow()).collect { operation ->
                val partition = operation.partition
                val initialStateJson = operation.initialState
                val exceptions = mutableListOf<String>()
                var stackTrace = ""
                var currentState: Any? = null
                var lastSuccessfulSequenceNumber = 0L

                // Deserialize initial state if present
                if (initialStateJson.isNotBlank() && readModelClass != null) {
                    try {
                        currentState = gson.fromJson(initialStateJson, readModelClass.java)
                    } catch (_: Exception) {}
                }

                for (appendedEvent in operation.eventsList) {
                    val eventTypeId = appendedEvent.context.eventType.id
                    val handlerPair = handlersByEventTypeId[eventTypeId]
                    if (handlerPair != null) {
                        val (fn, eventKClass) = handlerPair
                        try {
                            val event = gson.fromJson(appendedEvent.content, eventKClass.java)
                            val params = fn.parameters
                            currentState = when (params.size) {
                                2 -> fn.call(reducer, event)
                                3 -> fn.call(reducer, event, currentState)
                                else -> fn.call(reducer, event)
                            }
                            lastSuccessfulSequenceNumber = appendedEvent.context.sequenceNumber
                        } catch (e: Exception) {
                            exceptions.add(e.message ?: "Error in ${fn.name}")
                            stackTrace = e.stackTraceToString()
                        }
                    } else {
                        lastSuccessfulSequenceNumber = appendedEvent.context.sequenceNumber
                    }
                }

                val resultState = if (exceptions.isEmpty())
                    ObservationReducers.ObservationState.Success
                else
                    ObservationReducers.ObservationState.Failed

                val readModelStateJson = if (currentState != null) gson.toJson(currentState) else ""

                requests.send(
                    ObservationReducers.ReducerMessage.newBuilder()
                        .setContent(
                            ObservationReducers.OneOf_RegisterReducer_ReducerResult.newBuilder()
                                .setValue1(
                                    ObservationReducers.ReducerResult.newBuilder()
                                        .setPartition(partition)
                                        .setState(resultState)
                                        .setLastSuccessfulObservation(lastSuccessfulSequenceNumber)
                                        .addAllExceptionMessages(exceptions)
                                        .setExceptionStackTrace(stackTrace)
                                        .setReadModelState(readModelStateJson)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
        }
    }
}
