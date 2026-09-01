// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

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
     * @return The matching [ReadModelSnapshot]s, with their read model instance deserialized into [T].
     */
    suspend fun <T : Any> getSnapshotsById(readModelClass: KClass<T>, key: String): List<ReadModelSnapshot<T>>

    /**
     * Observe changes for a specific read model instance.
     *
     * @param readModelClass The read model type.
     * @return A [Flow] of [ReadModelChangeset], with the read model instance deserialized into [T].
     */
    fun <T : Any> watch(readModelClass: KClass<T>): Flow<ReadModelChangeset<T>>

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
     * @param subject The compliance subject to release for. Defaults to a property annotated
     *   [io.cratis.chronicle.Subject], falling back to the instance's `id` property, if present.
     * @return The decrypted instance, or the original when release is not applicable or fails.
     */
    suspend fun <T : Any> release(instance: T, subject: String? = null): T

    /**
     * Release (decrypt) PII-annotated properties in a collection of read model instances.
     *
     * The subject is derived from each instance individually - see [release] for details.
     *
     * @param instances The read model instances to decrypt.
     * @return The decrypted instances, in the same order as [instances].
     */
    suspend fun <T : Any> releaseMany(instances: List<T>): List<T>

    /** Provides paginated access to server-materialized read model instances. */
    val materialized: IMaterializedReadModels
}
