// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Counts every event into a dictionary-typed property, with the key resolved from [EventContext].
 * Each event increments the counter for the key produced by [keyFromContext].
 *
 * Applies to all event types the projection observes. The annotated property must be a
 * `MutableMap<String, Long>` or compatible dictionary type.
 *
 * @property keyFromContext The [EventContext] property name that provides the dictionary key
 *   (e.g., "eventType.id", "correlationId", "causedBy").
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CountFromAll(
    val keyFromContext: String
)
