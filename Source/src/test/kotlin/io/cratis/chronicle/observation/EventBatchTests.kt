// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.observation.Batches.event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class EventBatchTests {
    private val batch = EventBatch(
        listOf(event(1, "a", AccountOpened("ann")), event(2, "b", AccountOpened("bob")), event(3, "a", MoneyDeposited(1)))
            .map { EventAndContext(it.content, it.context) },
    )

    @Test
    fun `the partitions are listed in the order their first event appears`() {
        assertEquals(listOf("a", "b"), batch.partitions)
    }

    @Test
    fun `failing a partition reports it from its first event in the batch`() {
        batch.fail("a", IllegalStateException("x"))
        assertEquals(1L, batch.outcomes.single().at)
    }

    @Test
    fun `deferring an event reports its partition from that event`() {
        batch.defer(batch[2].context, Duration.ofMillis(5))
        val outcome = batch.outcomes.single()
        assertEquals(BatchOutcome.Kind.Deferred, outcome.kind)
        assertEquals(3L, outcome.at)
    }

    @Test
    fun `reporting a partition the batch holds no events for is refused`() {
        assertThrows<PartitionNotInBatch> { batch.fail("z", IllegalStateException("x")) }
    }
}
