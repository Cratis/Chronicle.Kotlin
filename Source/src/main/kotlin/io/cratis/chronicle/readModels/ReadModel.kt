// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

/**
 * Marks a class as a Chronicle read model.
 *
 * The observer type (Reducer or Projection) and the observer identifier are derived automatically
 * from whichever [io.cratis.chronicle.observation.IReducersService] or
 * [io.cratis.chronicle.projections.IProjectionsService] registers this read model — there is no
 * need to specify them here.
 *
 * @property id Explicit identifier. Defaults to the class simple name.
 * @property displayName Human-readable label. Defaults to the class simple name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadModel(
    val id: String = "",
    val displayName: String = ""
)
