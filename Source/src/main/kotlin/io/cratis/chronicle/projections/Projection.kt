// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Marks a class as a Chronicle projection.
 *
 * @property id Explicit identifier. Defaults to the class's fully-qualified name.
 * @property readModel The read model class this projection builds. Defaults to [Any] (inferred from [IProjectionFor]).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Projection(
    val id: String = "",
    val readModel: KClass<*> = Any::class
)
