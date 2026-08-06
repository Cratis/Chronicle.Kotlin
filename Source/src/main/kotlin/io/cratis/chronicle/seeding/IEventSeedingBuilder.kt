// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import kotlin.reflect.KClass

/**
 * Defines a builder for seeding events in the event store.
 */
interface IEventSeedingBuilder {
    /**
     * Seed events for a specific event source id with multiple event types.
     * By default, seed data applies to the event store's own namespace.
     *
     * @param eventSourceId The event source id to seed for.
     * @param events The events to seed.
     * @return The builder for continuation.
     */
    fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingBuilder

    /**
     * Seed events of a specific type for an event source id.
     * By default, seed data applies to the event store's own namespace.
     *
     * @param eventClass The event type being seeded. Must be annotated with `@EventType`.
     * @param eventSourceId The event source id to seed for.
     * @param events The events to seed. All events must be of type [TEvent].
     * @return The builder for continuation.
     */
    fun <TEvent : Any> forEventType(eventClass: KClass<TEvent>, eventSourceId: String, events: List<TEvent>): IEventSeedingBuilder

    /**
     * Configure seed data to target a specific namespace.
     *
     * @param namespace The namespace to seed for.
     * @return A scoped builder for namespace-specific seed data.
     */
    fun forNamespace(namespace: String): IEventSeedingScopeBuilder
}
