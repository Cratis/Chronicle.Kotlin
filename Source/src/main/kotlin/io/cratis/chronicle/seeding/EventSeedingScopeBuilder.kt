// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Represents an implementation of [IEventSeedingScopeBuilder], scoping every entry added through it
 * to a specific namespace on the owning [EventSeedingBuilder].
 */
internal class EventSeedingScopeBuilder(
    private val parent: EventSeedingBuilder,
    private val namespace: String
) : IEventSeedingScopeBuilder {

    override fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingScopeBuilder {
        parent.addScopedEntry(eventSourceId, events, namespace)
        return this
    }

    override fun <TEvent : Any> forEventType(eventClass: KClass<TEvent>, eventSourceId: String, events: List<TEvent>): IEventSeedingScopeBuilder {
        require(eventClass.findAnnotation<EventType>() != null) {
            "${eventClass.simpleName} must be annotated with @EventType to be seeded"
        }
        return forEventSource(eventSourceId, events)
    }
}
