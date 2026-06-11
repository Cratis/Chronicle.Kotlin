// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import com.google.gson.Gson
import kotlin.reflect.KClass

interface IReadModelsService {
    suspend fun register(vararg readModelClasses: KClass<*>)
    suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T?
}
