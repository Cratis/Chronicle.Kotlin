// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Like [RemovedWith], but the removal event doesn't directly carry the id — it's resolved via a
 * join instead.
 *
 * @property eventType The event class that triggers the removal.
 * @property key The event property identifying the instance/child to remove, via the join. Defaults to "EventSourceId".
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class RemovedWithJoin(
    val eventType: KClass<*>,
    val key: String = "EventSourceId"
)
