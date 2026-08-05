// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import com.google.gson.Gson
import io.cratis.chronicle.compliance.ComplianceService
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

private val gson = Gson()

/** Resolves the read model identifier for [this] class: the [ReadModel.id] override, or the class simple name. */
internal fun KClass<*>.readModelIdentifier(): String {
    val ann = findAnnotation<ReadModel>()
    return ann?.id?.ifEmpty { simpleName!! } ?: simpleName!!
}

class ReadModelsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: ReadModelsGrpcKt.ReadModelsCoroutineStub,
    materializedStub: MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub,
    complianceStub: ComplianceGrpcKt.ComplianceCoroutineStub,
    private val defaultSinkTypeId: String = WellKnownSinkTypes.MONGODB
) : IReadModelsService {
    private val compliance = ComplianceService(eventStoreName, namespace, complianceStub)

    override val materialized: IMaterializedReadModels = MaterializedReadModels(eventStoreName, namespace, materializedStub)

    override suspend fun register(vararg readModelClasses: KClass<*>) {
        for (cls in readModelClasses) {
            registerWithObserver(cls, 0, "")
        }
    }

    /**
     * Registers a read model and associates it with the given observer type and identifier.
     * Called internally by [io.cratis.chronicle.observation.ReducersService] and
     * [io.cratis.chronicle.projections.ProjectionsService] so that observer info is derived
     * from the reducer/projection rather than from the [ReadModel] annotation.
     *
     * @param cls Read model class to register.
     * @param observerType 0 = NotSet, 1 = Reducer, 2 = Projection.
     * @param observerIdentifier Simple name of the reducer or projection that produces this model.
     */
    internal suspend fun registerWithObserver(cls: KClass<*>, observerType: Int, observerIdentifier: String) {
        val ann = cls.findAnnotation<ReadModel>()
        val identifier = cls.readModelIdentifier()
        val displayName = ann?.displayName?.ifEmpty { cls.simpleName!! } ?: cls.simpleName!!

        val definition = Readmodels.ReadModelDefinition.newBuilder()
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
                    .setTypeId(defaultSinkTypeId)
                    .build()
            )
            .setSchema(JsonSchemaGenerator.generate(cls))
            .setObserverTypeValue(observerType)
            .setObserverIdentifier(observerIdentifier)
            .build()

        val request = Readmodels.RegisterManyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addReadModels(definition)
            .setSourceValue(1)
            .build()

        stub.registerMany(request)
    }

    override suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T? {
        val request = Readmodels.GetInstanceByKeyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
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

    override suspend fun <T : Any> getInstances(readModelClass: KClass<T>, eventCount: Long?): List<T> {
        val builder = Readmodels.GetAllInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
        if (eventCount != null) builder.setEventCount(eventCount)

        return stub.getAllInstances(builder.build()).instancesList.map { gson.fromJson(it, readModelClass.java) }
    }

    override suspend fun getSnapshotsById(readModelClass: KClass<*>, key: String): List<Readmodels.ReadModelSnapshot> {
        val request = Readmodels.GetSnapshotsByKeyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .build()

        return stub.getSnapshotsByKey(request).snapshotsList
    }

    override fun watch(readModelClass: KClass<*>): Flow<Readmodels.ReadModelChangeset> {
        val request = Readmodels.WatchRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .build()

        return stub.watch(request)
    }

    override suspend fun dehydrateSession(readModelClass: KClass<*>, key: String, sessionId: String) {
        val request = Readmodels.DehydrateSessionRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .setSessionId(sessionId)
            .build()

        stub.dehydrateSession(request)
    }

    override suspend fun <T : Any> release(instance: T, subject: String?): T {
        val resolvedSubject = subject ?: resolveSubject(instance) ?: return instance
        val schema = JsonSchemaGenerator.generate(instance::class)
        val payload = gson.toJson(instance)
        val released = compliance.release(resolvedSubject, schema, payload)
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(released, instance::class.java) as T
    }

    /** Resolves the compliance subject for [instance] by looking for an `id` property, falling back to none. */
    private fun resolveSubject(instance: Any): String? =
        instance::class.memberProperties
            .firstOrNull { it.name.equals("id", ignoreCase = true) }
            ?.call(instance)
            ?.toString()
}
