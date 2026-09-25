// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import Cratis.Chronicle.Contracts.Seeding.EventSeedingGrpcKt
import Cratis.Chronicle.Contracts.Seeding.Seeding
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import kotlin.reflect.full.findAnnotation

class EventSeedingService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: EventSeedingGrpcKt.EventSeedingCoroutineStub
) : IEventSeedingService {

    override suspend fun seed(vararg seeders: Any) {
        // Entries with no IEventSeedingBuilder.forNamespace() are global: the kernel applies them to
        // every namespace of the event store, including namespaces created later. Entries scoped with
        // forNamespace() go to that namespace only. This is the same split the .NET client makes.
        val globalEntries = mutableListOf<Seeding.SeedingEntry>()
        val entriesByNamespace = linkedMapOf<String, MutableList<Seeding.EventSourceSeedEntries>>()

        for (seeder in seeders) {
            if (seeder !is ICanSeedEvents) continue
            val builder = EventSeedingBuilder()
            seeder.seed(builder)

            for (entry in builder.build()) {
                val seedingEntries = entry.events.mapNotNull { event ->
                    val ann = event::class.findAnnotation<EventType>() ?: return@mapNotNull null
                    val eventTypeId = ann.id.ifEmpty { event::class.simpleName!! }
                    Seeding.SeedingEntry.newBuilder()
                        .setEventSourceId(entry.eventSourceId)
                        .setEventTypeId(eventTypeId)
                        .setContent(chronicleGson.toJson(event))
                        .build()
                }
                if (seedingEntries.isEmpty()) continue

                val targetNamespace = entry.namespace
                if (targetNamespace == null) {
                    globalEntries.addAll(seedingEntries)
                } else {
                    entriesByNamespace.getOrPut(targetNamespace) { mutableListOf() }.add(
                        Seeding.EventSourceSeedEntries.newBuilder()
                            .setEventSourceId(entry.eventSourceId)
                            .addAllEntries(seedingEntries)
                            .build()
                    )
                }
            }
        }

        if (globalEntries.isEmpty() && entriesByNamespace.isEmpty()) return

        val requestBuilder = Seeding.SeedEventsRequest.newBuilder().setEventStore(eventStoreName)

        // Global entries are indexed both by event type and by event source, as the kernel expects.
        globalEntries.groupBy { it.eventTypeId }.forEach { (eventTypeId, entries) ->
            requestBuilder.addGlobalByEventType(
                Seeding.EventTypeSeedEntries.newBuilder()
                    .setEventTypeId(eventTypeId)
                    .addAllEntries(entries)
                    .build()
            )
        }
        globalEntries.groupBy { it.eventSourceId }.forEach { (eventSourceId, entries) ->
            requestBuilder.addGlobalByEventSource(
                Seeding.EventSourceSeedEntries.newBuilder()
                    .setEventSourceId(eventSourceId)
                    .addAllEntries(entries)
                    .build()
            )
        }

        entriesByNamespace.forEach { (targetNamespace, eventSourceEntries) ->
            requestBuilder.addNamespacedEntries(
                Seeding.NamespacedSeedEntries.newBuilder()
                    .setNamespace(targetNamespace)
                    .addAllByEventSource(eventSourceEntries)
                    .build()
            )
        }

        stub.seedEvents(requestBuilder.build())
    }
}
