// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

/**
 * Represents a single seeding entry collected by [EventSeedingBuilder].
 *
 * @property eventSourceId The event source id the [events] are seeded for.
 * @property events The events to seed.
 * @property namespace The namespace to target, or `null` to use the event store's own namespace -
 *   set via [IEventSeedingBuilder.forNamespace].
 */
data class EventSeedEntry(
    val eventSourceId: String,
    val events: List<Any>,
    val namespace: String? = null
)
