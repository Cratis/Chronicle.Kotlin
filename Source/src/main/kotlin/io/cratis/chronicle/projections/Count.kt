// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Turns a property into an occurrence counter for a specific event type — every time [eventType]
 * fires for the read model instance, the property is bumped by one.
 *
 * @property eventType The event class to count occurrences of.
 * @property constantKey When set, every occurrence of [eventType] updates the same read model
 *   instance, identified by this constant value, instead of the projection's normal key resolution.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Count(
    val eventType: KClass<*>,
    val constantKey: String = ""
)
