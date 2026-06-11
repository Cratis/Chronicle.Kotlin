// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import Cratis.Chronicle.Contracts.Seeding.EventSeedingGrpcKt
import Cratis.Chronicle.Contracts.Seeding.Seeding
import com.google.gson.Gson
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

private val gson = Gson()

class EventSeedingService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: EventSeedingGrpcKt.EventSeedingCoroutineStub
) : IEventSeedingService {

    override suspend fun seed(vararg seeders: Any) {
        val allEventSourceEntries = mutableListOf<Seeding.EventSourceSeedEntries>()

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
                        .setContent(gson.toJson(event))
                        .build()
                }

                if (seedingEntries.isNotEmpty()) {
                    allEventSourceEntries.add(
                        Seeding.EventSourceSeedEntries.newBuilder()
                            .setEventSourceId(entry.eventSourceId)
                            .addAllEntries(seedingEntries)
                            .build()
                    )
                }
            }
        }

        if (allEventSourceEntries.isEmpty()) return

        val namespacedEntries = Seeding.NamespacedSeedEntries.newBuilder()
            .setNamespace(namespace)
            .addAllByEventSource(allEventSourceEntries)
            .build()

        val request = Seeding.SeedRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addNamespacedEntries(namespacedEntries)
            .build()

        stub.seed(request)
    }
}
