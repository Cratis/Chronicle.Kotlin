// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class EventStoreSubscriptionBuilder : IEventStoreSubscriptionBuilder {
    private val eventTypeIds = mutableListOf<String>()

    override fun <TEvent : Any> withEventType(eventClass: KClass<TEvent>): IEventStoreSubscriptionBuilder {
        val ann = eventClass.findAnnotation<EventType>() ?: return this
        eventTypeIds.add(ann.id.ifEmpty { eventClass.simpleName!! })
        return this
    }

    fun build(): List<String> = eventTypeIds.toList()
}
