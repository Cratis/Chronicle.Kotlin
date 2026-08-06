// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import io.cratis.chronicle.json.chronicleGson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

class MaterializedReadModels(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub
) : IMaterializedReadModels {

    override suspend fun <T : Any> getInstances(readModelClass: KClass<T>, skip: Int, take: Int): List<T> {
        val request = Readmodels.GetInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModel(readModelClass.readModelIdentifier())
            .setPage(skip / take.coerceAtLeast(1) + 1)
            .setPageSize(take)
            .build()

        return stub.getInstances(request).instancesList.map { chronicleGson.fromJson(it, readModelClass.java) }
    }

    override fun <T : Any> observeInstances(readModelClass: KClass<T>, skip: Int, take: Int): Flow<List<T>> {
        val request = Readmodels.ObserveInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModel(readModelClass.readModelIdentifier())
            .setPage(skip / take.coerceAtLeast(1) + 1)
            .setPageSize(take)
            .build()

        return stub.observeInstances(request).map { response ->
            response.instancesList.map { chronicleGson.fromJson(it, readModelClass.java) }
        }
    }
}
