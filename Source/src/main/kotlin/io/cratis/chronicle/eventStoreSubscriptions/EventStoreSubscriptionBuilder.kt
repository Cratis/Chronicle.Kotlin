// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class EventStoreSubscriptionBuilder : IEventStoreSubscriptionBuilder {
    private val eventTypes = mutableListOf<EventTypeDescriptor>()

    override fun <TEvent : Any> withEventType(eventClass: KClass<TEvent>): IEventStoreSubscriptionBuilder {
        val ann = eventClass.findAnnotation<EventType>() ?: return this
        val id = ann.id.ifEmpty { eventClass.simpleName!! }
        eventTypes.add(EventTypeDescriptor(EventTypeId(id), EventTypeGeneration(ann.generation), ann.tombstone))
        return this
    }

    fun build(): List<EventTypeDescriptor> = eventTypes.toList()
}
