// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

private class SomeSource

/** Callers written before event sources existed: the old constructor and `copy` shapes must keep working. */
class ApiCompatibilityTests {
    private val context = EventContext(
        sequenceNumber = 1,
        eventSourceId = "id",
        eventType = EventTypeDescriptor(EventTypeId("E"), EventTypeGeneration(1)),
        occurred = Instant.EPOCH,
        correlationId = UUID.randomUUID(),
        causedBy = Identity.unknown,
        eventSource = "Account"
    )

    @Test
    fun `the sixteen argument event context constructor still works`() {
        val legacy = EventContext(
            1, "id", EventTypeDescriptor(EventTypeId("E"), EventTypeGeneration(1)), Instant.EPOCH, UUID.randomUUID(),
            Identity.unknown, "a", "b", "c", "store", "ns", emptyList(), listOf("t"), "h", EventObservationState.none, "subject"
        )

        assertEquals("", legacy.eventSource)
        assertEquals("subject", legacy.subject)
    }

    @Test
    fun `copying an event context the old way keeps the event source`() {
        val copied = context.copy(
            context.sequenceNumber, context.eventSourceId, context.eventType, context.occurred, context.correlationId,
            context.causedBy, "a", "b", "c", "store", "ns", emptyList(), emptyList(), "h", EventObservationState.none, "s"
        )

        assertEquals("Account", copied.eventSource)
        assertEquals("ns", copied.namespace)
    }

    @Test
    fun `copying an event context by name keeps the event source`() {
        assertEquals("Account", context.copy(namespace = "other").eventSource)
    }

    @Test
    fun `copying append options the old way keeps the routing`() {
        val options = AppendOptions(eventSource = SomeSource::class, eventStream = "Stream")

        val copied = options.copy(null, null, "Type", null, null, null, emptyList(), null, emptyList())

        assertEquals(SomeSource::class, copied.eventSource)
        assertEquals("Stream", copied.eventStream)
        assertEquals("Type", copied.eventSourceType)
    }

    @Test
    fun `the nine argument append options constructor leaves routing unset`() {
        val options = AppendOptions(null, null, null, null, null, null, emptyList(), null, emptyList())

        assertNull(options.eventSource)
        assertNull(options.eventStream)
    }

    @Test
    fun `copying an event for event source id the old way keeps the routing`() {
        val event = EventForEventSourceId("id", Any(), eventSource = SomeSource::class, eventStream = "Stream")

        val copied = event.copy("other", event.event, null, null, null, emptyList(), null, null, emptyList())

        assertEquals("other", copied.eventSourceId)
        assertEquals(SomeSource::class, copied.eventSource)
        assertEquals("Stream", copied.eventStream)
    }

    @Test
    fun `the nine argument event for event source id constructor leaves routing unset`() {
        val event = EventForEventSourceId("id", Any(), null, null, null, emptyList(), null, null, emptyList())

        assertNull(event.eventSource)
    }

    @Test
    fun `the synthetic default argument bridges of the previous release are still generated`() {
        // Kotlin callers compiled against the previous release call these synthetic members directly when
        // they rely on default arguments. Removing them is a binary break even though source still compiles.
        val appendOptions = AppendOptions::class.java
        val eventFor = EventForEventSourceId::class.java
        val context = EventContext::class.java

        assertEquals(1, appendOptions.defaultConstructorsWithParameterCount(9 + 2))
        assertEquals(1, eventFor.defaultConstructorsWithParameterCount(9 + 2))
        assertEquals(1, context.defaultConstructorsWithParameterCount(16 + 2))
        assertEquals(1, EventLog::class.java.defaultConstructorsWithParameterCount(6 + 2))
        assertEquals(1, EventSequence::class.java.defaultConstructorsWithParameterCount(6 + 2))
        assertEquals(1, appendOptions.copyDefaultsWithParameterCount(1 + 9 + 2))
        assertEquals(1, eventFor.copyDefaultsWithParameterCount(1 + 9 + 2))
        assertEquals(1, context.copyDefaultsWithParameterCount(1 + 16 + 2))
    }

    @Test
    fun `a default argument construction and copy of the previous shape ignores event source routing`() {
        assertNull(AppendOptions().eventSource)
        assertNull(AppendOptions(subject = "s").copy(tags = listOf("t")).eventSource)
        assertEquals(listOf("t"), EventForEventSourceId("id", Any()).copy(tags = listOf("t")).tags)
    }

    @Test
    fun `routing arguments select the new primary shape and survive a legacy copy`() {
        val routed = AppendOptions(eventSource = SomeSource::class, eventStream = "Stream")

        val copied = routed.copy(subject = "s")

        assertEquals(SomeSource::class, copied.eventSource)
        assertEquals("Stream", copied.eventStream)
        assertEquals(SomeSource::class, routed.copy(eventSource = SomeSource::class).eventSource)
    }

    private fun Class<*>.defaultConstructorsWithParameterCount(count: Int) =
        declaredConstructors.count {
            it.parameterCount == count && it.isSynthetic && it.parameterTypes[count - 2] == Int::class.javaPrimitiveType
        }

    private fun Class<*>.copyDefaultsWithParameterCount(count: Int) =
        declaredMethods.count { it.name == "copy\$default" && it.parameterCount == count }
}
