// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface IReadModelsService {
    suspend fun register(vararg readModelClasses: KClass<*>)
    suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T?

    /**
     * Get all instances of a read model by replaying events, in-process.
     *
     * @param readModelClass The read model type.
     * @param eventCount Optional maximum number of events to process. Defaults to unlimited.
     * @return The read model instances.
     */
    suspend fun <T : Any> getInstances(readModelClass: KClass<T>, eventCount: Long? = null): List<T>

    /**
     * Get snapshots of a read model grouped by correlation id, by walking through events from the beginning.
     *
     * @param readModelClass The read model type.
     * @param key The read model key to get snapshots for.
     * @return The matching snapshots.
     */
    suspend fun getSnapshotsById(readModelClass: KClass<*>, key: String): List<Readmodels.ReadModelSnapshot>

    /**
     * Observe changes for a specific read model instance.
     *
     * @param readModelClass The read model type.
     * @return A [Flow] of [Readmodels.ReadModelChangeset].
     */
    fun watch(readModelClass: KClass<*>): Flow<Readmodels.ReadModelChangeset>

    /**
     * Dehydrate a session, releasing any session-scoped state held for a read model instance.
     *
     * @param readModelClass The read model type.
     * @param key The read model key to dehydrate for.
     * @param sessionId The session identifier to dehydrate.
     */
    suspend fun dehydrateSession(readModelClass: KClass<*>, key: String, sessionId: String)

    /**
     * Release (decrypt) PII-annotated properties in a read model instance.
     *
     * @param instance The read model instance to decrypt.
     * @param subject The compliance subject to release for. Defaults to the instance's `id` property, if present.
     * @return The decrypted instance, or the original when release is not applicable or fails.
     */
    suspend fun <T : Any> release(instance: T, subject: String? = null): T

    /** Provides paginated access to server-materialized read model instances. */
    val materialized: IMaterializedReadModels
}
