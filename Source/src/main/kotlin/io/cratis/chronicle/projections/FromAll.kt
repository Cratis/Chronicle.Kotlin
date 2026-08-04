// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Projects a property from every event type the projection observes, rather than a single one.
 *
 * Equivalent to [FromEvery] — both populate the same catch-all mapping on the projection.
 *
 * @property property The property on the triggering event to read the value from.
 *   Defaults to the annotated property's own name.
 * @property contextProperty The event context property to read the value from instead of an
 *   event property (e.g. the causing identity). Takes precedence over [property] when set.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FromAll(
    val property: String = "",
    val contextProperty: String = ""
)
