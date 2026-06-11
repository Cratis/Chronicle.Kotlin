// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Maps a read model property from a specific event property path.
 *
 * @property propertyPath Dot-separated path to the source property on the event. Defaults to the annotated property's own name.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SetFrom(val propertyPath: String = "")
