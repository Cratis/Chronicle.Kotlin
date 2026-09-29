// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

/**
 * Decides the name of the container - the MongoDB collection or SQL table - a read model is stored in.
 *
 * The policy only names the container. The read model's identifier, which projections, reducers and
 * observers refer to it by, is not affected. Set one on
 * [io.cratis.chronicle.ChronicleOptions.withReadModelNamingPolicy] when something else reads the
 * container by name and expects a convention the identifier does not follow - a pluralized collection
 * name, for example.
 */
fun interface ReadModelNamingPolicy {
    /**
     * Gets the container name for a read model.
     *
     * @param readModelClass The read model class.
     * @return The container name.
     */
    fun getReadModelName(readModelClass: Class<*>): String
}

/**
 * The policy used when none is configured: the container is named after the read model's identifier,
 * which is the [ReadModel.id] override or otherwise the class simple name.
 */
object DefaultReadModelNamingPolicy : ReadModelNamingPolicy {
    override fun getReadModelName(readModelClass: Class<*>): String = readModelClass.kotlin.readModelIdentifier()
}
