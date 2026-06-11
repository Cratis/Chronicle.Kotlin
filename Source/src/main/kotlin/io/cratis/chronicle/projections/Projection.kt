// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Marks a class as a Chronicle projection, or overrides the projection identifier on a model-bound read model.
 *
 * This annotation is optional. When omitted, the class simple name is used as the projection identifier.
 * Use it only when the identifier must differ from the class name (e.g. after a rename).
 *
 * For declarative projections the read model type is inferred from the [IProjectionFor] type parameter.
 * For model-bound projections the annotated class itself is the read model.
 *
 * @property id Explicit identifier. Defaults to the class simple name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Projection(val id: String = "")
