// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.observation.Batches.event
import io.cratis.chronicle.observation.Batches.page
import io.cratis.chronicle.observation.Batches.partition
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * A reducer folds each event source of a page from its stored state: its batch method is called once
 * per event source, and a page's event sources are reduced at the same time, up to a bound.
 */
class ReducerPageDispatchTests {
    @Reducer
    class AccountReducer {
        val calls = mutableListOf<Pair<String, List<Long>>>()
        var onBatch: (EventBatch) -> Unit = {}

        @Observes(AccountOpened::class)
        @Observes(MoneyDeposited::class)
        @Observes(MoneyWithdrawn::class)
        fun reduce(events: EventBatch, current: AccountBalance?, context: BatchContext): AccountBalance? {
            synchronized(calls) { calls += context.observerId to events.map { it.context.sequenceNumber } }
            onBatch(events)
            return events.fold(current) { account, e ->
                when (val event = e.event) {
                    is AccountOpened -> AccountBalance(event.owner, 0)
                    is MoneyDeposited -> account!!.copy(balance = account.balance + event.amount)
                    is MoneyWithdrawn -> account!!.copy(balance = account.balance - event.amount)
                    else -> account
                }
            }
        }
    }

    @Reducer
    class PerEventAccountReducer {
        fun opened(event: AccountOpened, @Suppress("UNUSED_PARAMETER") current: AccountBalance?) = AccountBalance(event.owner, 0)

        fun deposited(event: MoneyDeposited, current: AccountBalance?): AccountBalance {
            if (event.amount < 0) throw IllegalArgumentException("negative deposit")
            return current!!.copy(balance = current.balance + event.amount)
        }
    }

    @Reducer(maxConcurrency = 2)
    class SlowReducer {
        val running = AtomicInteger()
        val peak = AtomicInteger()

        @Observes(AccountOpened::class)
        suspend fun reduce(events: EventBatch, @Suppress("UNUSED_PARAMETER") current: AccountBalance?): AccountBalance {
            peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
            delay(50)
            running.decrementAndGet()
            return AccountBalance((events.single().event as AccountOpened).owner)
        }
    }

    private fun observe(reducer: Any, page: DeliveredPage): List<PartitionOutcome> = runBlocking {
        val registration = ReducerRegistration.from(reducer::class)
        ReducerEventDispatch(registration, ReducerFold(registration, reducer)).observe(page, Batches.contextFor(page))
    }

    private fun stateOf(outcome: PartitionOutcome): AccountBalance? =
        outcome.readModelState?.let { chronicleGson.fromJson(it, AccountBalance::class.java) }

    @Test
    fun `the batch method is called once per event source, from its stored state`() {
        val reducer = AccountReducer()
        val outcomes = observe(
            reducer,
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(3, "a", MoneyDeposited(10))),
                partition("b", event(2, "b", MoneyDeposited(5)), initialState = chronicleGson.toJson(AccountBalance("bob", 1))),
            ),
        )

        assertEquals(setOf(listOf(1L, 3L), listOf(2L)), reducer.calls.map { it.second }.toSet())
        assertEquals(listOf(AccountBalance("ann", 10), AccountBalance("bob", 6)), outcomes.map(::stateOf))
        assertEquals(listOf(3L, 2L), outcomes.map { it.lastSuccessfulObservation })
        assertTrue(outcomes.all { it.state == PartitionState.Success })
    }

    @Test
    fun `per-event handlers fold each event source of a page`() {
        val outcomes = observe(
            PerEventAccountReducer(),
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(3, "a", MoneyDeposited(10))),
                partition("b", event(2, "b", AccountOpened("bob"))),
            ),
        )

        assertEquals(listOf(AccountBalance("ann", 10), AccountBalance("bob", 0)), outcomes.map(::stateOf))
    }

    @Test
    fun `a failing fold writes no state and fails only its event source`() {
        val outcomes = observe(
            PerEventAccountReducer(),
            page(
                partition("a", event(1, "a", AccountOpened("ann")), event(3, "a", MoneyDeposited(-1))),
                partition("b", event(2, "b", AccountOpened("bob"))),
            ),
        )

        val (a, b) = outcomes
        assertEquals(PartitionState.Failed, a.state)
        assertEquals(PartitionOutcome.NONE_OBSERVED, a.lastSuccessfulObservation)
        assertNull(a.readModelState)
        assertEquals(listOf("negative deposit"), a.messages)
        assertEquals(PartitionState.Success, b.state)
    }

    @Test
    fun `failing an event source from the batch fails it without state`() {
        val reducer = AccountReducer().apply { onBatch = { batch -> batch.fail(batch.first().context, IllegalStateException("nope")) } }
        val outcome = observe(reducer, page(partition("a", event(1, "a", AccountOpened("ann"))))).single()

        assertEquals(PartitionState.Failed, outcome.state)
        assertNull(outcome.readModelState)
        assertEquals(listOf("nope"), outcome.messages)
    }

    @Test
    fun `deferring an event source defers it with the delay asked for`() {
        val reducer = AccountReducer().apply { onBatch = { batch -> batch.defer("a", Duration.ofSeconds(2)) } }
        val outcome = observe(reducer, page(partition("a", event(1, "a", AccountOpened("ann"))))).single()

        assertEquals(PartitionState.Deferred, outcome.state)
        assertEquals(Duration.ofSeconds(2), outcome.retryAfter)
        assertNull(outcome.readModelState)
    }

    @Test
    fun `the event sources of a page are reduced at the same time, up to the bound`() {
        val reducer = SlowReducer()
        val outcomes = observe(
            reducer,
            page(*(1..6).map { partition("p$it", event(it.toLong(), "p$it", AccountOpened("o$it"))) }.toTypedArray()),
        )

        assertEquals(2, reducer.peak.get())
        assertEquals((1..6).map { AccountBalance("o$it") }, outcomes.map(::stateOf))
    }
}
