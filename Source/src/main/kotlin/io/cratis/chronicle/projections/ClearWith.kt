// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares which event clears (nulls out) a [Nested] single-object property.
 *
 * Placed on the nested type itself, alongside its [FromEvent] annotation.
 *
 * @property eventType The event class that clears the nested object.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ClearWith(
    val eventType: KClass<*>
)
