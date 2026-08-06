// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Bumps a numeric property by one every time [eventType] fires for the read model instance.
 *
 * @property eventType The event class that increments the property.
 * @property constantKey When set, every occurrence of [eventType] updates the same read model
 *   instance, identified by this constant value, instead of the projection's normal key resolution.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Increment(
    val eventType: KClass<*>,
    val constantKey: String = ""
)
