// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares event handlers once for every variant of [identity], rather than repeating them on each
 * [VariantOf] type.
 *
 * The decorated type carries the shared mappings as ordinary model-bound members - for example a
 * property with [SetFrom]. Every mapping is merged into every variant of [identity], and every
 * variant must have the members the mappings target; one that does not is a declaration error
 * rather than a silently skipped mapping.
 *
 * A shared handler can only ever update an already-active variant. It is merged as an update-only,
 * self-referential join, so an event handled globally never creates a variant and can never
 * resurrect the entity into a variant it has since left.
 *
 * A type carrying only [GlobalFor] is never registered as its own projection - it exists purely to
 * be merged into its siblings.
 *
 * @property identity The type that anchors the logical identity the variants are grouped under.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GlobalFor(val identity: KClass<*>)
