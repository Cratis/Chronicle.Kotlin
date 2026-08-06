// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/** Provides paginated access to read model instances materialized server-side (e.g. by a sink). */
interface IMaterializedReadModels {
    /**
     * Get a paginated window of instances of a read model from the sink.
     *
     * @param readModelClass The read model type.
     * @param skip Number of instances to skip. Defaults to zero.
     * @param take Number of instances to retrieve. Defaults to 50.
     * @return The read model instances.
     */
    suspend fun <T : Any> getInstances(readModelClass: KClass<T>, skip: Int = 0, take: Int = 50): List<T>

    /**
     * Observe a paginated window of read model instances as it changes.
     *
     * @param readModelClass The read model type.
     * @param skip Number of instances to skip. Defaults to zero.
     * @param take Number of instances to observe. Defaults to 50.
     * @return A [Flow] emitting the paginated window every time it changes.
     */
    fun <T : Any> observeInstances(readModelClass: KClass<T>, skip: Int = 0, take: Int = 50): Flow<List<T>>
}
