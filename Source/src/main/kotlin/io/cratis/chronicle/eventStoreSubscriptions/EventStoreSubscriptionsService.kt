// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.EventStoreSubscriptionsGrpcKt
import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions
import io.cratis.chronicle.events.IEventTypesService

class EventStoreSubscriptionsService(
    private val eventStoreName: String,
    private val stub: EventStoreSubscriptionsGrpcKt.EventStoreSubscriptionsCoroutineStub,
    private val eventTypes: IEventTypesService
) : IEventStoreSubscriptionsService {

    override suspend fun subscribe(id: String, sourceEventStore: String, configure: (IEventStoreSubscriptionBuilder) -> Unit) {
        val builder = EventStoreSubscriptionBuilder()
        configure(builder)

        // When the caller never narrows to specific event types, subscribe to everything this
        // client has registered - rather than sending an empty list, which would subscribe to
        // nothing at all.
        val selectedEventTypes = builder.build().ifEmpty { eventTypes.getRegisteredEventTypes() }

        val definition = ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition.newBuilder()
            .setIdentifier(id)
            .setSourceEventStore(sourceEventStore)
            .addAllEventTypes(
                selectedEventTypes.map { eventType ->
                    ObservationEventstoresubscriptions.EventType.newBuilder()
                        .setId(eventType.id.value)
                        .setGeneration(eventType.generation.value)
                        .setTombstone(eventType.tombstone)
                        .build()
                }
            )
            .build()

        val request = ObservationEventstoresubscriptions.AddEventStoreSubscriptions.newBuilder()
            .setTargetEventStore(eventStoreName)
            .addSubscriptions(definition)
            .build()

        stub.add(request)
    }

    override suspend fun unsubscribe(id: String) {
        val request = ObservationEventstoresubscriptions.RemoveEventStoreSubscriptions.newBuilder()
            .setTargetEventStore(eventStoreName)
            .addSubscriptionIds(id)
            .build()

        stub.remove(request)
    }

    override suspend fun getAll(): List<ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition> {
        val request = ObservationEventstoresubscriptions.GetEventStoreSubscriptionsRequest.newBuilder()
            .setTargetEventStore(eventStoreName)
            .build()

        return stub.getSubscriptions(request).itemsList
    }
}
