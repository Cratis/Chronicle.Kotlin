// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions

interface IEventStoreSubscriptionsService {
    /**
     * Subscribe to events from a source event store's outbox.
     *
     * @param id The unique identifier for this subscription.
     * @param sourceEventStore The name of the source event store to subscribe to.
     * @param configure The callback for configuring the subscription (e.g. filter event types).
     */
    suspend fun subscribe(id: String, sourceEventStore: String, configure: (IEventStoreSubscriptionBuilder) -> Unit)

    /**
     * Remove a subscription by its identifier.
     *
     * @param id The identifier of the subscription to remove.
     */
    suspend fun unsubscribe(id: String)

    /**
     * Get all subscriptions registered for this event store.
     */
    suspend fun getAll(): List<ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition>
}
