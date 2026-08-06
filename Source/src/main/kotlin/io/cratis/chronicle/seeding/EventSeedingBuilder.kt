// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class EventSeedingBuilder : IEventSeedingBuilder {
    private val entries = mutableListOf<EventSeedEntry>()

    override fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingBuilder {
        entries.add(EventSeedEntry(eventSourceId, events))
        return this
    }

    override fun <TEvent : Any> forEventType(eventClass: KClass<TEvent>, eventSourceId: String, events: List<TEvent>): IEventSeedingBuilder {
        require(eventClass.findAnnotation<EventType>() != null) {
            "${eventClass.simpleName} must be annotated with @EventType to be seeded"
        }
        return forEventSource(eventSourceId, events)
    }

    override fun forNamespace(namespace: String): IEventSeedingScopeBuilder = EventSeedingScopeBuilder(this, namespace)

    fun build(): List<EventSeedEntry> = entries.toList()

    /** Adds an entry targeting a specific namespace - used by [EventSeedingScopeBuilder]. */
    internal fun addScopedEntry(eventSourceId: String, events: List<Any>, namespace: String) {
        entries.add(EventSeedEntry(eventSourceId, events, namespace))
    }
}
