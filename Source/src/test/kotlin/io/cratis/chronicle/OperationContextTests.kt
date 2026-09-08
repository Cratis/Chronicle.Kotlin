// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.java.BlockingEventSequence
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OperationContextTests {
    @Test
    fun `constructor defensively copies causation and its properties`() {
        val properties = linkedMapOf("one" to "1")
        val chain = mutableListOf(Causation(Instant.EPOCH, CausationType("test"), properties))
        val context = OperationContext(UUID.randomUUID(), chain, Identity.system)

        properties["two"] = "2"
        chain.clear()

        assertEquals(mapOf("one" to "1"), context.causation.single().properties)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (context.causation as MutableList<Causation>).clear()
        }
    }

    @Test
    fun `copy retains values and can replace one naturally`() {
        val original = OperationContext.system()
        val identity = Identity("u1", "User")
        val copied = original.copy(causedBy = identity)

        assertEquals(original.correlationId, copied.correlationId)
        assertEquals(identity, copied.causedBy)
    }

    @Test
    fun `java builder creates explicit context`() {
        val correlationId = UUID.randomUUID()
        val context = OperationContextJavaUsage.build(correlationId)

        assertEquals(correlationId, context.correlationId)
        assertEquals("java-user", context.causedBy.subject)
        assertEquals("JavaCall", context.causation.single().type.name)
    }

    @Test
    fun `java transaction stages across sources and commits once`() {
        val sequence = mockk<IEventSequence>()
        coEvery {
            sequence.appendMany(
                any<List<EventForEventSourceId>>(),
                any<OperationContext>(),
                any<Map<String, ConcurrencyScope>>()
            )
        } returns listOf(
            AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true),
            AppendResult(EventSequenceNumber(1), emptyList(), emptyList(), true)
        )
        val context = OperationContextJavaUsage.build(UUID.randomUUID())

        OperationContextJavaUsage.transact(BlockingEventSequence(sequence), context, "one", "two")

        coVerify(exactly = 1) {
            sequence.appendMany(
                match { it.map(EventForEventSourceId::eventSourceId) == listOf("first", "second") },
                context,
                any<Map<String, ConcurrencyScope>>()
            )
        }
    }

    @Test
    fun `system contexts are fresh per call`() {
        assertNotEquals(OperationContext.system().correlationId, OperationContext.system().correlationId)
    }
}
