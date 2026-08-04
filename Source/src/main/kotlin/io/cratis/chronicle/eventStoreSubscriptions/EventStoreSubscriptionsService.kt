// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.EventStoreSubscriptionsGrpcKt
import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions

class EventStoreSubscriptionsService(
    private val eventStoreName: String,
    private val stub: EventStoreSubscriptionsGrpcKt.EventStoreSubscriptionsCoroutineStub
) : IEventStoreSubscriptionsService {

    override suspend fun subscribe(id: String, sourceEventStore: String, configure: (IEventStoreSubscriptionBuilder) -> Unit) {
        val builder = EventStoreSubscriptionBuilder()
        configure(builder)

        val definition = ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition.newBuilder()
            .setIdentifier(id)
            .setSourceEventStore(sourceEventStore)
            .addAllEventTypes(
                builder.build().map { eventTypeId ->
                    ObservationEventstoresubscriptions.EventType.newBuilder()
                        .setId(eventTypeId)
                        .setGeneration(1)
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
