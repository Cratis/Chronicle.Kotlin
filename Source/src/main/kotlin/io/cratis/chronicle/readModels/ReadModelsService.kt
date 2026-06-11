// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.EventSequenceId
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

private const val SINK_TYPE_MONGODB = "MongoDB"

private val gson = Gson()

class ReadModelsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: ReadModelsGrpcKt.ReadModelsCoroutineStub
) : IReadModelsService {

    override suspend fun register(vararg readModelClasses: KClass<*>) {
        val definitions = readModelClasses.map { cls ->
            val ann = cls.findAnnotation<ReadModel>()
            val identifier = ann?.id?.ifEmpty { cls.simpleName!! } ?: cls.simpleName!!
            val displayName = ann?.displayName?.ifEmpty { cls.simpleName!! } ?: cls.simpleName!!
            val observerTypeValue = ann?.observerTypeValue ?: 0
            val observerIdentifier = if (ann?.observerClass != null && ann.observerClass != Nothing::class) {
                ann.observerClass.simpleName ?: ""
            } else ""

            Readmodels.ReadModelDefinition.newBuilder()
                .setType(
                    Readmodels.ReadModelType.newBuilder()
                        .setIdentifier(identifier)
                        .build()
                )
                .setContainerName(identifier)
                .setDisplayName(displayName)
                .setSink(
                    Readmodels.SinkDefinition.newBuilder()
                        // lo=1 ensures the field serializes on the wire so C# protobuf-net
                        // sees a non-null Guid (Guid.Empty maps to 00000001-... on server side,
                        // which is used only as a configuration reference key).
                        .setConfigurationId(
                            Bcl.Guid.newBuilder().setLo(1L).setHi(0L).build()
                        )
                        .setTypeId(SINK_TYPE_MONGODB)
                        .build()
                )
                .setSchema(generateSchema(cls))
                .setObserverTypeValue(observerTypeValue)
                .setObserverIdentifier(observerIdentifier)
                .build()
        }

        val request = Readmodels.RegisterManyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addAllReadModels(definitions)
            .setSourceValue(1)
            .build()

        stub.registerMany(request)
    }

    override suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T? {
        val ann = readModelClass.findAnnotation<ReadModel>()
        val identifier = ann?.id?.ifEmpty { readModelClass.simpleName!! }
            ?: readModelClass.simpleName!!

        val request = Readmodels.GetInstanceByKeyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(identifier)
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .setSessionId("")
            .build()

        val response = stub.getInstanceByKey(request)
        return if (response.readModel.isNullOrBlank() || response.readModel == "null") {
            null
        } else {
            gson.fromJson(response.readModel, readModelClass.java)
        }
    }

    /**
     * Generates a minimal NJsonSchema-compatible JSON schema from a data class's member properties.
     * The server uses the schema to infer which property is the read model key —
     * it looks for "id" or "Id" first, then falls back to camel/pascal-case heuristics.
     */
    private fun generateSchema(cls: KClass<*>): String {
        val properties = cls.memberProperties.associate { prop ->
            val typeName = when (prop.returnType.classifier) {
                String::class -> "string"
                Int::class, Long::class, Short::class, Byte::class -> "integer"
                Double::class, Float::class -> "number"
                Boolean::class -> "boolean"
                else -> "string"
            }
            prop.name to mapOf("type" to typeName)
        }

        val schema = mapOf(
            "type" to "object",
            "properties" to properties
        )
        return gson.toJson(schema)
    }
}
