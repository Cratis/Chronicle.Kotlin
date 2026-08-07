// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.readModels.IReadModelsService
import kotlinx.coroutines.runBlocking

/**
 * Read models for Java, without the coroutines - and without `JvmClassMappingKt`.
 *
 * The suspending surface takes a Kotlin `KClass`, which Java has no comfortable way to produce.
 * These take a plain `Class` instead.
 *
 * @param readModels The read models to forward to.
 */
class BlockingReadModels(private val readModels: IReadModelsService) {

    /** The suspending service underneath, for anything this does not cover. */
    fun unwrap(): IReadModelsService = readModels

    /**
     * The instance of [readModelClass] held under [key], or `null` when nothing has been projected
     * for that key yet.
     *
     * @param readModelClass The read model to look up.
     * @param key The key it is held under - typically the event source id.
     */
    fun <T : Any> getInstanceByKey(readModelClass: Class<T>, key: String): T? =
        runBlocking { readModels.getInstanceByKey(readModelClass.kotlin, key) }

    /** Registers read models that no observer produces. */
    fun register(vararg readModelClasses: Class<*>) {
        runBlocking { readModels.register(*readModelClasses.map { it.kotlin }.toTypedArray()) }
    }
}
