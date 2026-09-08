// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@EventType
private data class ReactorSideEffect(val value: String)

class ReactorSideEffectsTests {
    @Test
    fun `side effects propagate triggering correlation causation and identity as one batch`() = runBlocking {
        val eventLog = mockk<IEventLog>()
        coEvery {
            eventLog.appendMany(
                any<List<EventForEventSourceId>>(),
                any<OperationContext>(),
                any<Map<String, ConcurrencyScope>>()
            )
        } returns listOf(AppendResult(EventSequenceNumber(1), emptyList(), emptyList(), true))
        val causation = listOf(Causation(Instant.EPOCH, CausationType("trigger")))
        val identity = Identity("user", "User")
        val context = EventContext(
            sequenceNumber = 0,
            eventSourceId = "source",
            eventType = EventTypeDescriptor(EventTypeId("Trigger")),
            occurred = Instant.EPOCH,
            correlationId = UUID.randomUUID(),
            causedBy = identity,
            causation = causation
        )

        ReactorSideEffects(eventLog).append(ReactorSideEffect("result"), context)

        coVerify(exactly = 1) {
            eventLog.appendMany(
                match { it.single().eventSourceId == "source" },
                match {
                    it.correlationId == context.correlationId &&
                        it.causation == causation &&
                        it.causedBy == identity
                },
                any<Map<String, ConcurrencyScope>>()
            )
        }
    }
}
