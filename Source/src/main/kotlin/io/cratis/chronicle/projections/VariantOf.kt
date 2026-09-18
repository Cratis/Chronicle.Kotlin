// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares that a read model is one of several mutually exclusive representations of the same
 * logical entity. Every type marked with this annotation for the same [identity] forms a group:
 * entering one variant (see [EntersOn]) removes the entity from every other variant in the group.
 *
 * Unlike an ordinary [FromEvent] key - which only ever correlates one event to a read model instance -
 * [key] names a property that actually holds the variant's own identity (typically mapped with
 * [FromEventSourceId]). Every mapping the variant declares for an event besides its entering one is
 * reclassified into a join back onto this property, so it needs to resolve to the same value the
 * entering event created the row with.
 *
 * @property identity The type that anchors the logical identity shared by every variant in the
 *   group. It does not need to be a read model itself, and it does not need a common supertype
 *   with any of the variants.
 * @property key The property on this read model that carries the shared identity.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VariantOf(val identity: KClass<*>, val key: String)
