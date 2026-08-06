// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events.migrations

import kotlin.reflect.KClass

/**
 * Defines a migration between two generations of the same event type.
 *
 * Both [targetClass] and [sourceClass] must be annotated with `@EventType`, sharing the same
 * id but differing generations, with [targetClass] being exactly one generation ahead of
 * [sourceClass].
 *
 * @param TTarget The upgraded (newer generation) event type.
 * @param TSource The previous (older generation) event type.
 */
interface IEventTypeMigration<TTarget : Any, TSource : Any> {
    /** The [KClass] of the upgraded (newer generation) event type. */
    val targetClass: KClass<TTarget>

    /** The [KClass] of the previous (older generation) event type. */
    val sourceClass: KClass<TSource>

    /**
     * Define the upcast migration from [TSource] to [TTarget].
     *
     * @param builder The [EventTypeMigrationBuilder] to describe the property transformations with.
     */
    fun upcast(builder: EventTypeMigrationBuilder<TTarget, TSource>) {}

    /**
     * Define the downcast migration from [TTarget] to [TSource].
     *
     * @param builder The [EventTypeMigrationBuilder] to describe the property transformations with.
     */
    fun downcast(builder: EventTypeMigrationBuilder<TSource, TTarget>) {}
}
