// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.observation.Batches.event
import io.cratis.chronicle.observation.Batches.page
import io.cratis.chronicle.observation.Batches.partition
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * A reactor is handed a page of many partitions. Per-event handlers see each partition's events in
 * order; a batch method is called once with every event it observes; and each partition settles at
 * the earliest event any method could not handle.
 */
class ReactorPageDispatchTests {
    class PerEventReactor {
        val seen = mutableListOf<String>()

        fun opened(event: AccountOpened) {
            seen += "opened:${event.owner}"
        }

        fun deposited(event: MoneyDeposited) {
            if (event.amount < 0) throw IllegalArgumentException("negative deposit")
            seen += "deposited:${event.amount}"
        }
    }

    class MixedReactor {
        val batches = mutableListOf<List<Long>>()
        val perEvent = mutableListOf<Long>()
        var context: BatchContext? = null
        var onBatch: (EventBatch) -> Unit = {}

        @Observes(AccountOpened::class)
        @Observes(MoneyDeposited::class)
        suspend fun index(batch: EventBatch, context: BatchContext) {
            batches += batch.map { it.context.sequenceNumber }
            this.context = context
            onBatch(batch)
        }

        fun noted(@Suppress("UNUSED_PARAMETER") event: AccountNoted, context: io.cratis.chronicle.events.EventContext) {
            perEvent += context.sequenceNumber
            if (context.eventSourceId == "failing-note") throw IllegalStateException("note failed")
        }
    }

    class OnceOnlyBatchReactor {
        val batches = mutableListOf<List<Long>>()

        @OnceOnly
        @Observes(AccountOpened::class)
        fun index(batch: EventBatch) {
            batches += batch.map { it.context.sequenceNumber }
        }
    }

    class ReplayAwareReactor : ICanBeNotifiedAboutReplay {
        val calls = mutableListOf<String>()

        @Observes(AccountOpened::class)
        fun index(batch: EventBatch) {
            calls += "batch:${batch.size}"
        }

        fun replayBegan(context: ReplayContext) {
            calls += "began:${context.partition}"
        }

        fun replayEnded(context: ReplayContext) {
            calls += "ended:${context.partition}"
            throw IllegalStateException("cache is gone")
        }
    }

    private fun <T : Any> dispatchFor(reactor: T) = ReactorEventDispatch(
        ReactorRegistration.from(reactor::class),
        reactor,
        ReactorMiddlewares.none,
        ReactorMethodArguments.contextOnly,
        ReactorSideEffects(mockk<IEventLog>()),
    )

    private fun observe(reactor: Any, page: DeliveredPage) = runBlocking {
        dispatchFor(reactor).observe(page, Batches.contextFor(page))
    }

    @Test
    fun `per-event handlers see each partition of a page in order`() {
        val reactor = PerEventReactor()
        val outcomes = observe(
            reactor,
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(3, "a", MoneyDeposited(10))),
                partition("b", event(2, "b", AccountOpened("bob"))),
            ),
        )

        assertEquals(listOf("opened:ann", "deposited:10", "opened:bob"), reactor.seen)
        assertEquals(listOf("a", "b"), outcomes.map { it.partition })
        assertTrue(outcomes.all { it.state == PartitionState.Success })
        assertEquals(listOf(3L, 2L), outcomes.map { it.lastSuccessfulObservation })
    }

    @Test
    fun `a failing per-event handler stops only its own partition, at the event that failed`() {
        val reactor = PerEventReactor()
        val outcomes = observe(
            reactor,
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(3, "a", MoneyDeposited(-1)), event(4, "a", MoneyDeposited(5))),
                partition("b", event(2, "b", AccountOpened("bob"))),
            ),
        )

        val (a, b) = outcomes
        assertEquals(PartitionState.Failed, a.state)
        assertEquals(1L, a.lastSuccessfulObservation)
        assertEquals(listOf("negative deposit"), a.messages)
        assertEquals(PartitionState.Success, b.state)
        // The event after the failure is left for when the partition is delivered again.
        assertEquals(listOf("opened:ann", "opened:bob"), reactor.seen)
    }

    @Test
    fun `a batch method is called once with the events it observes across partitions, in sequence order`() {
        val reactor = MixedReactor()
        val page = page(
            partition("a", event(1, "a", AccountOpened("ann")), event(2, "a", AccountNoted("x")), event(5, "a", MoneyDeposited(3))),
            partition("b", event(3, "b", AccountOpened("bob")), event(4, "b", MoneyDeposited(7))),
            origin = ObservationOrigin.CatchUp,
        )
        val outcomes = observe(reactor, page)

        assertEquals(listOf(listOf(1L, 3L, 4L, 5L)), reactor.batches)
        assertEquals(listOf(2L), reactor.perEvent)
        assertTrue(outcomes.all { it.state == PartitionState.Success })
        assertEquals(listOf(5L, 4L), outcomes.map { it.lastSuccessfulObservation })

        val context = reactor.context!!
        assertEquals(ObservationOrigin.CatchUp, context.origin)
        assertEquals(listOf("a", "b"), context.partitions)
        assertEquals(5, context.eventCount)
        assertEquals(1L, context.firstSequenceNumber)
        assertEquals(5L, context.lastSequenceNumber)
    }

    @Test
    fun `failing an event fails its partition from that event and leaves the rest of the batch`() {
        val reactor = MixedReactor().apply {
            onBatch = { batch -> batch.fail(batch.single { it.context.sequenceNumber == 4L }.context, IllegalStateException("index rejected")) }
        }
        val outcomes = observe(
            reactor,
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(5, "a", MoneyDeposited(3))),
                partition("b", event(3, "b", AccountOpened("bob")), event(4, "b", MoneyDeposited(7)), event(6, "b", MoneyDeposited(1))),
            ),
        )

        val (a, b) = outcomes
        assertEquals(PartitionState.Success, a.state)
        assertEquals(PartitionState.Failed, b.state)
        assertEquals(3L, b.lastSuccessfulObservation)
        assertEquals(listOf("index rejected"), b.messages)
    }

    @Test
    fun `deferring a partition defers it from its first event, without failing it`() {
        val reactor = MixedReactor().apply { onBatch = { batch -> batch.defer("b", Duration.ofSeconds(30)) } }
        val outcomes = observe(
            reactor,
            page(
                partition("a", event(1, "a", AccountOpened("ann"))),
                partition("b", event(2, "b", AccountOpened("bob")), event(3, "b", MoneyDeposited(7))),
            ),
        )

        val b = outcomes[1]
        assertEquals(PartitionState.Deferred, b.state)
        assertEquals(PartitionOutcome.NONE_OBSERVED, b.lastSuccessfulObservation)
        assertEquals(Duration.ofSeconds(30), b.retryAfter)
        assertTrue(b.messages.isEmpty())
        assertEquals(PartitionState.Success, outcomes[0].state)
    }

    @Test
    fun `a deferral is a failure when the kernel did not deliver a page`() {
        val reactor = MixedReactor().apply { onBatch = { batch -> batch.defer("a", Duration.ofSeconds(30)) } }
        val outcome = observe(reactor, page(partition("a", event(1, "a", AccountOpened("ann"))), isPage = false)).single()

        assertEquals(PartitionState.Failed, outcome.state)
        assertTrue(outcome.messages.single().contains("does not accept deferred partitions"))
    }

    @Test
    fun `a throwing batch method fails each of its partitions at its first event in the batch`() {
        val reactor = MixedReactor().apply { onBatch = { throw IllegalStateException("index is down") } }
        val outcomes = observe(
            reactor,
            page(
                partition("a", event(1, "a", AccountNoted("x")), event(2, "a", AccountOpened("ann"))),
                partition("b", event(3, "b", AccountOpened("bob"))),
                partition("c", event(4, "c", AccountNoted("y"))),
            ),
        )

        assertEquals(listOf(PartitionState.Failed, PartitionState.Failed, PartitionState.Success), outcomes.map { it.state })
        assertEquals(listOf(1L, PartitionOutcome.NONE_OBSERVED, 4L), outcomes.map { it.lastSuccessfulObservation })
        assertEquals("index is down", outcomes[0].messages.single())
    }

    @Test
    fun `a partition settles at the earliest problem any method reported`() {
        val reactor = MixedReactor().apply { onBatch = { batch -> batch.fail("failing-note", IllegalStateException("late")) } }
        val outcome = observe(
            reactor,
            page(
                partition(
                    "failing-note",
                    event(1, "failing-note", AccountOpened("ann")),
                    event(2, "failing-note", AccountNoted("x")),
                    event(3, "failing-note", MoneyDeposited(4)),
                ),
            ),
        ).single()

        // The batch failed the partition from event 1, the per-event handler at event 2: 1 wins.
        assertEquals(PartitionState.Failed, outcome.state)
        assertEquals(PartitionOutcome.NONE_OBSERVED, outcome.lastSuccessfulObservation)
        assertEquals(listOf("late"), outcome.messages)
    }

    @Test
    fun `a once-only batch method is left out of a replay`() {
        val reactor = OnceOnlyBatchReactor()
        val replayed = EventObservationState(EventObservationState.REPLAY)
        val outcome = observe(
            reactor,
            page(partition("a", event(1, "a", AccountOpened("ann"), replayed), event(2, "a", AccountOpened("ann"), replayed))),
        ).single()

        assertTrue(reactor.batches.isEmpty())
        assertEquals(PartitionState.Success, outcome.state)
        assertEquals(2L, outcome.lastSuccessfulObservation)
    }

    @Test
    fun `replay notifications bracket the page, and a failing one fails its partition at that event`() {
        val reactor = ReplayAwareReactor()
        val head = EventObservationState(EventObservationState.REPLAY or EventObservationState.HEAD_OF_REPLAY)
        val tail = EventObservationState(EventObservationState.REPLAY or EventObservationState.TAIL_OF_REPLAY)
        val outcome = observe(
            reactor,
            page(partition("a", event(1, "a", AccountOpened("ann"), head), event(2, "a", AccountOpened("ann"), tail))),
        ).single()

        assertEquals(listOf("began:a", "batch:2", "ended:a"), reactor.calls)
        assertEquals(PartitionState.Failed, outcome.state)
        assertEquals(1L, outcome.lastSuccessfulObservation)
    }
}
