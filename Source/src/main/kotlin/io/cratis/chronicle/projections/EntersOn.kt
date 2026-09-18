// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares the event that activates (creates) a [VariantOf] variant.
 *
 * Only the event(s) declared with this annotation may create or resurrect the variant. Every other
 * event handled by the variant - whether declared directly on it with [FromEvent] or shared through
 * a [GlobalFor] handler - only updates an already-active instance of this variant and can never
 * create one, so it can never resurrect the entity into a variant it has since left.
 *
 * @property eventType The type of event that activates this variant.
 * @property key The property on the event that identifies the read model instance. Defaults to
 *   "EventSourceId".
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class EntersOn(
    val eventType: KClass<*>,
    val key: String = "EventSourceId"
)
