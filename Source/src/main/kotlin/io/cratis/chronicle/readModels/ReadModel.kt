// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import kotlin.reflect.KClass

/**
 * Marks a class as a Chronicle read model.
 *
 * @property id Explicit identifier. Defaults to the class's fully-qualified name.
 * @property displayName Human-readable label. Defaults to the simple class name.
 * @property observerTypeValue Observer type: 0 = NotSet, 1 = Reducer, 2 = Projection.
 * @property observerClass The reducer or projection class that produces this read model.
 *   Leave at [Nothing] when there is no associated observer.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadModel(
    val id: String = "",
    val displayName: String = "",
    val observerTypeValue: Int = 0,
    val observerClass: KClass<*> = Nothing::class
)
