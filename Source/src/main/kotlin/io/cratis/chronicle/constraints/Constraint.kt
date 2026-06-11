// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Marks a class as a Chronicle constraint.
 *
 * @property id Explicit identifier. Defaults to the class's fully-qualified name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Constraint(val id: String = "")
