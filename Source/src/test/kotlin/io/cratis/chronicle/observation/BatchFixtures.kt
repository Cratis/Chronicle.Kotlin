// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.json.chronicleGson
import java.time.Instant
import java.util.UUID

@EventType
data class AccountOpened(val owner: String)

@EventType
data class MoneyDeposited(val amount: Int)

@EventType
data class MoneyWithdrawn(val amount: Int)

@EventType
data class AccountNoted(val note: String)

data class AccountBalance(val owner: String = "", val balance: Int = 0)

/** Not an event type: it carries no @EventType. */
data class NotAnEvent(val value: String)

/** Builds the delivered events the batch specs feed to dispatchers. */
internal object Batches {
    fun event(
        sequenceNumber: Long,
        eventSourceId: String,
        event: Any,
        observationState: EventObservationState = EventObservationState.initial,
    ): AppendedEvent = AppendedEvent(
        EventContext(
            sequenceNumber = sequenceNumber,
            eventSourceId = eventSourceId,
            eventType = EventTypeDescriptor(EventTypeId(event::class.simpleName!!), EventTypeGeneration.first),
            occurred = Instant.EPOCH,
            correlationId = UUID(0L, 0L),
            causedBy = Identity.system,
            observationState = observationState,
        ),
        chronicleGson.toJson(event),
    )

    fun page(vararg partitions: DeliveredPartition, isPage: Boolean = true, origin: ObservationOrigin = ObservationOrigin.Live) =
        DeliveredPage(partitions.toList(), origin, 0L, isPage)

    fun partition(id: String, vararg events: AppendedEvent, initialState: String = "") =
        DeliveredPartition(id, events.toList(), initialState)

    fun contextFor(page: DeliveredPage): BatchContext = page.contextFor("observer", "store", "Default", "event-log")
}
