// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Increments a dictionary-typed property for each event, with the key resolved from [EventContext].
 * Each event adds 1 to the value for the key produced by [keyFromContext].
 *
 * Applies to all event types the projection observes. The annotated property must be a
 * `MutableMap<String, Long>` or compatible dictionary type.
 *
 * Equivalent to [CountFromAll] — both increment the counter for the resolved key.
 *
 * @property keyFromContext The [EventContext] property name that provides the dictionary key
 *   (e.g., "eventType.id", "correlationId", "causedBy").
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IncrementFromAll(
    val keyFromContext: String
)
