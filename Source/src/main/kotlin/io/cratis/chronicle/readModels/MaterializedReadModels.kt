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
        val paging = calculatePaging(skip, take)
        val request = Readmodels.GetInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModel(readModelClass.readModelIdentifier())
            .setPage(paging.page)
            .setPageSize(paging.pageSize)
            .build()

        return stub.getInstances(request).instancesList
            .map { chronicleGson.fromJson(it, readModelClass.java) }
            .applyLocalSlice(paging)
    }

    override fun <T : Any> observeInstances(readModelClass: KClass<T>, skip: Int, take: Int): Flow<List<T>> {
        val paging = calculatePaging(skip, take)
        val request = Readmodels.ObserveInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModel(readModelClass.readModelIdentifier())
            .setPage(paging.page)
            .setPageSize(paging.pageSize)
            .build()

        return stub.observeInstances(request).map { response ->
            response.instancesList
                .map { chronicleGson.fromJson(it, readModelClass.java) }
                .applyLocalSlice(paging)
        }
    }
}

/**
 * The server page to request for a skip/take window, and the local slicing to apply to the response.
 *
 * @property page The zero-based server page to request.
 * @property pageSize The server page size to request.
 * @property localSkip The number of instances to skip in the response.
 * @property localTake The maximum number of instances to keep from the response.
 */
internal data class MaterializedPaging(val page: Int, val pageSize: Int, val localSkip: Int, val localTake: Int)

/** The take value that requests every instance, mirroring `InstanceCount.Unlimited` in the .NET client. */
internal const val UNLIMITED_TAKE = Int.MAX_VALUE

/**
 * Translates a requested [skip]/[take] window into the server's zero-based, page-aligned paging contract
 * plus the local slicing needed to return the exact window.
 *
 * The server only exposes page-aligned offsets (its effective offset is always `page * pageSize`). When [skip]
 * is a multiple of [take] the window is exactly one server page, so the page request is used directly. When it
 * is not, the window `[skip, skip + take)` straddles two server pages; a covering range is fetched from the
 * start and sliced locally so the full [take] items are returned. This mirrors `CalculatePaging` in the .NET client.
 */
internal fun calculatePaging(skip: Int, take: Int): MaterializedPaging {
    val skipCount = skip.coerceAtLeast(0)

    // Unlimited take: fetch everything from the start and skip locally.
    if (take == UNLIMITED_TAKE) {
        return MaterializedPaging(0, Int.MAX_VALUE, skipCount, Int.MAX_VALUE)
    }

    // A take of zero (or less) requests no instances - fetch nothing (and never divide by zero below).
    if (take <= 0) {
        return MaterializedPaging(0, 0, 0, 0)
    }

    // Page-aligned skip: the requested window is exactly one server page.
    if (skipCount % take == 0) {
        return MaterializedPaging(skipCount / take, take, 0, take)
    }

    // Non-aligned skip: the window straddles two server pages. Fetch a covering range from the start
    // (page size guarded against int overflow) and slice it locally to the requested window.
    val coveringPageSize = minOf(Int.MAX_VALUE.toLong(), skipCount.toLong() + take).toInt()
    return MaterializedPaging(0, coveringPageSize, skipCount, take)
}

private fun <T> List<T>.applyLocalSlice(paging: MaterializedPaging): List<T> = drop(paging.localSkip).take(paging.localTake)
