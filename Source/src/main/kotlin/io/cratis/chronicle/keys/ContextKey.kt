// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

/**
 * Marks a function as deriving the projection key for an event from the event context rather than
 * from a property on the event payload itself - for example, the event source id, or a correlation
 * id, neither of which is a property on the event.
 *
 * @property property The name of the [io.cratis.chronicle.events.EventContext] property to use as
 * the key.
 *
 * This mirrors .NET's `Cratis.Chronicle.Keys.ContextKeyAttribute`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContextKey(val property: String)
