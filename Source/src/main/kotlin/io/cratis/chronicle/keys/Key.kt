// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

/**
 * Marks a property on an event type as the key a projection correlates that event to a read model
 * instance by.
 *
 * Today, [io.cratis.chronicle.projections.FromEvent.key] takes the key as a bare string - the
 * property name, or the default `"EventSourceId"` - which is easy to typo and impossible for a
 * refactor to catch. [Key] is the strongly-typed alternative: put it directly on the property that
 * should act as the correlation key, and a consumer scanning the event type (rather than a
 * hand-written string) can resolve it without either side repeating the property's name.
 *
 * ```kotlin
 * @EventType
 * data class OrderLineAdded(@Key val orderId: String, val product: String, val quantity: Int)
 * ```
 *
 * This mirrors .NET's `Cratis.Chronicle.Keys.KeyAttribute`.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Key
