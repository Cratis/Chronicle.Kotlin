// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a class as a Chronicle reducer.
 *
 * @property id Explicit identifier. Defaults to the class simple name.
 * @property eventSequence The identifier of the event sequence to observe.
 *   Defaults to the event log when not specified.
 * @property isActive Whether the reducer runs actively on the kernel. A passive reducer is
 *   registered but not run, so its read model is only produced on demand.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reducer(
    val id: String = "",
    val eventSequence: String = "",
    val isActive: Boolean = true
)
