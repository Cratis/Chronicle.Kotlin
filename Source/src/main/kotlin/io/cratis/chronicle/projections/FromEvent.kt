// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares that a read model is projected from a specific event type.
 *
 * @property eventType The event class to project from.
 * @property key The key expression used to correlate events to read model instances. Defaults to "EventSourceId".
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class FromEvent(
    val eventType: KClass<*>,
    val key: String = "EventSourceId"
)
