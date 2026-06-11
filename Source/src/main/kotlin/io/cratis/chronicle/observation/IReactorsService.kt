// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlinx.coroutines.Job

interface IReactorsService {
    suspend fun register(reactor: Any): Job
}
