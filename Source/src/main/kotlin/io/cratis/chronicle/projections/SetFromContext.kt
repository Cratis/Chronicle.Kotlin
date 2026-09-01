// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Maps a property from a named [io.cratis.chronicle.events.EventContext] property, for a specific event.
 *
 * This only ever reads the event context, never [eventType]'s own payload - but it still subscribes
 * the projection to [eventType]. Once subscribed, AutoMap becomes eligible for every one of
 * [eventType]'s payload properties against any same-named property on the read model, regardless of
 * which annotation caused the subscription. Fence an affected property with [NoAutoMap] when this is
 * not wanted.
 *
 * Unlike [FromAll]/[FromEvery], which map a context property from every event the projection observes,
 * this ties the mapping to a single [eventType].
 *
 * @property eventType The event class this context mapping applies to.
 * @property contextProperty The property on the event context to read the value from.
 *   Defaults to the annotated property's own name.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class SetFromContext(
    val eventType: KClass<*>,
    val contextProperty: String = ""
)
