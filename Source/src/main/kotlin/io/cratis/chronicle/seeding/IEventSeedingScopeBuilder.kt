// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import kotlin.reflect.KClass

/**
 * Defines a scoped builder for seeding events, targeting a specific namespace.
 *
 * Obtained from [IEventSeedingBuilder.forNamespace].
 */
interface IEventSeedingScopeBuilder {
    /**
     * Seed events for a specific event source id with multiple event types, targeting this
     * builder's namespace.
     *
     * @param eventSourceId The event source id to seed for.
     * @param events The events to seed.
     * @return The builder for continuation.
     */
    fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingScopeBuilder

    /**
     * Seed events of a specific type for an event source id, targeting this builder's namespace.
     *
     * @param eventClass The event type being seeded. Must be annotated with `@EventType`.
     * @param eventSourceId The event source id to seed for.
     * @param events The events to seed. All events must be of type [TEvent].
     * @return The builder for continuation.
     */
    fun <TEvent : Any> forEventType(eventClass: KClass<TEvent>, eventSourceId: String, events: List<TEvent>): IEventSeedingScopeBuilder
}
