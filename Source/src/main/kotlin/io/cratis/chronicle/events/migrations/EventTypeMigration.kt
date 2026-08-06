// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events.migrations

import kotlin.reflect.KClass

/**
 * Base class for type-safe event type migrations between two generations of the same event type.
 *
 * Extend this class and pass the target and source event classes to the constructor, then override
 * [upcast] and/or [downcast] to describe the property transformations between the two generations.
 * A migration discovered by [io.cratis.chronicle.events.EventTypesService] must have a public
 * no-argument constructor.
 *
 * @param TTarget The upgraded (newer generation) event type.
 * @param TSource The previous (older generation) event type.
 * @param targetClass The [KClass] of the upgraded event type.
 * @param sourceClass The [KClass] of the previous event type.
 */
abstract class EventTypeMigration<TTarget : Any, TSource : Any>(
    override val targetClass: KClass<TTarget>,
    override val sourceClass: KClass<TSource>
) : IEventTypeMigration<TTarget, TSource>
