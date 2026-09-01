// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Holds repeated [RemoveConstraint] annotations.
 *
 * Declared so that repeating [RemoveConstraint] works from Java as well as Kotlin. Apply
 * [RemoveConstraint] directly rather than using this.
 *
 * @property value The repeated [RemoveConstraint] annotations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RemoveConstraints(vararg val value: RemoveConstraint)
