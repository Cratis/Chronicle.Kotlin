// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a class as a Chronicle reducer.
 *
 * @property id Explicit identifier. Defaults to the class's fully-qualified name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reducer(val id: String = "")
