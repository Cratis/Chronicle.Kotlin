// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

class EventSeedingBuilder : IEventSeedingBuilder {
    private val entries = mutableListOf<EventSeedEntry>()

    override fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingBuilder {
        entries.add(EventSeedEntry(eventSourceId, events))
        return this
    }

    fun build(): List<EventSeedEntry> = entries.toList()
}
