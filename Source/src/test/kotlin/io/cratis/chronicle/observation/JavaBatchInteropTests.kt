// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.observation.Batches.event
import io.cratis.chronicle.observation.Batches.page
import io.cratis.chronicle.observation.Batches.partition
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

/** Batch methods written in Java: the repeated @Observes, EventBatch and BatchContext, from Java source. */
class JavaBatchInteropTests {
    @Test
    fun `a Java reactor's repeated observes registers each event type for its batch method`() {
        val handlers = ReactorHandlers.from(JavaBatchReactor::class)

        assertEquals(setOf("JavaBookAdded", "JavaBookRemoved", "JavaBookStored"), handlers.eventTypes.keys)
        assertEquals(setOf("JavaBookAdded", "JavaBookRemoved"), handlers.batches.single().eventTypes.keys)
    }

    @Test
    fun `a Java batch method is called with its events and can defer a partition`() = runBlocking {
        val reactor = JavaBatchReactor()
        val dispatch = ReactorEventDispatch(
            ReactorRegistration.from(JavaBatchReactor::class),
            reactor,
            ReactorMiddlewares.none,
            ReactorMethodArguments.contextOnly,
            ReactorSideEffects(mockk<IEventLog>()),
        )
        val delivered = page(
            partition("a", event(1, "a", JavaBookAdded("Dune")), event(3, "a", JavaBookStored("Dune"))),
            partition("b", event(2, "b", JavaBookAdded("throttled"))),
        )

        val outcomes = dispatch.observe(delivered, Batches.contextFor(delivered))

        assertEquals(listOf("added:Dune"), reactor.indexed)
        assertEquals(listOf("Dune"), reactor.confirmed)
        assertEquals(listOf(PartitionState.Success, PartitionState.Deferred), outcomes.map { it.state })
        assertEquals(Duration.ofSeconds(1), outcomes[1].retryAfter)
    }

    @Test
    fun `a Java batch reducer folds each event source`() = runBlocking {
        val registration = ReducerRegistration.from(JavaBatchReducer::class)
        val delivered = page(partition("a", event(1, "a", JavaBookAdded("Dune")), event(2, "a", JavaBookRemoved("Dune"))))

        val outcome = ReducerEventDispatch(registration, ReducerFold(registration, JavaBatchReducer()))
            .observe(delivered, Batches.contextFor(delivered))
            .single()

        assertEquals(JavaBookState::class, registration.readModelClass)
        assertEquals(1, registration.maxConcurrency)
        assertEquals(JavaBookState("Dune", false), chronicleGson.fromJson(outcome.readModelState, JavaBookState::class.java))
    }
}
