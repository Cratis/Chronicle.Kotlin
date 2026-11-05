// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Each event type reaches exactly one method, and a batch method says what it takes with @Observes.
 * Everything that would break either is refused when the observer registers.
 */
class BatchMethodRegistrationTests {
    class MixedReactor {
        @Observes(AccountOpened::class)
        @Observes(MoneyDeposited::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch, @Suppress("UNUSED_PARAMETER") context: BatchContext) = Unit

        fun noted(@Suppress("UNUSED_PARAMETER") event: AccountNoted) = Unit
    }

    class BatchOnlyReactor {
        @Observes(MoneyWithdrawn::class)
        suspend fun withdrawals(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit
    }

    class ObservesOnPerEventReactor {
        @Observes(AccountOpened::class)
        fun opened(@Suppress("UNUSED_PARAMETER") event: AccountOpened) = Unit
    }

    class ObservesNothingReactor {
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit
    }

    class ObservesNonEventReactor {
        @Observes(NotAnEvent::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit
    }

    class TwoBatchesOneTypeReactor {
        @Observes(AccountOpened::class)
        fun first(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit

        @Observes(AccountOpened::class)
        fun second(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit
    }

    class BatchAndPerEventOneTypeReactor {
        @Observes(AccountOpened::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit

        fun opened(@Suppress("UNUSED_PARAMETER") event: AccountOpened) = Unit
    }

    class PerEventTakingBatchContextReactor {
        fun opened(@Suppress("UNUSED_PARAMETER") event: AccountOpened, @Suppress("UNUSED_PARAMETER") context: BatchContext) = Unit
    }

    class ReturningBatchReactor {
        @Observes(AccountOpened::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch): List<Any> = emptyList()
    }

    class UnsuppliableBatchParameterReactor {
        @Observes(AccountOpened::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch, @Suppress("UNUSED_PARAMETER") context: EventContext) = Unit
    }

    class ReplayBatchReactor {
        @Replay
        @Observes(AccountOpened::class)
        fun index(@Suppress("UNUSED_PARAMETER") batch: EventBatch) = Unit
    }

    @Reducer
    class BatchReducer {
        @Observes(AccountOpened::class)
        @Observes(MoneyDeposited::class)
        fun reduce(@Suppress("UNUSED_PARAMETER") batch: EventBatch, current: AccountBalance?, @Suppress("UNUSED_PARAMETER") context: BatchContext): AccountBalance? = current
    }

    @Reducer(maxConcurrency = 3)
    class BoundedBatchReducer {
        @Observes(AccountOpened::class)
        fun reduce(@Suppress("UNUSED_PARAMETER") batch: EventBatch, current: AccountBalance?): AccountBalance? = current
    }

    @Reducer
    class TwoBatchReducer {
        @Observes(AccountOpened::class)
        fun first(@Suppress("UNUSED_PARAMETER") batch: EventBatch, current: AccountBalance?): AccountBalance? = current

        @Observes(MoneyDeposited::class)
        fun second(@Suppress("UNUSED_PARAMETER") batch: EventBatch, current: AccountBalance?): AccountBalance? = current
    }

    @Reducer
    class MixedReducer {
        @Observes(AccountOpened::class)
        fun reduce(@Suppress("UNUSED_PARAMETER") batch: EventBatch, current: AccountBalance?): AccountBalance? = current

        fun deposited(@Suppress("UNUSED_PARAMETER") event: MoneyDeposited, current: AccountBalance?): AccountBalance? = current
    }

    @Reducer
    class WrongStateReducer {
        @Observes(AccountOpened::class)
        fun reduce(@Suppress("UNUSED_PARAMETER") batch: EventBatch, @Suppress("UNUSED_PARAMETER") current: String?): AccountBalance? = null
    }

    @Reducer
    class PerEventTakingBatchContextReducer {
        fun opened(
            @Suppress("UNUSED_PARAMETER") event: AccountOpened,
            current: AccountBalance?,
            @Suppress("UNUSED_PARAMETER") context: BatchContext,
        ): AccountBalance? = current
    }

    @Test
    fun `a reactor registers the event types of its batch methods and its per-event handlers`() {
        val handlers = ReactorHandlers.from(MixedReactor::class)

        assertEquals(setOf("AccountOpened", "MoneyDeposited", "AccountNoted"), handlers.eventTypes.keys)
        assertEquals(listOf("index"), handlers.batches.map { it.name })
        assertEquals("index", handlers.batchFor("MoneyDeposited")?.name)
        assertNull(handlers.batchFor("AccountNoted"))
    }

    @Test
    fun `a reactor with only a batch method has handlers`() {
        assertEquals(setOf("MoneyWithdrawn"), ReactorHandlers.from(BatchOnlyReactor::class).eventTypes.keys)
    }

    @Test
    fun `observes on a method that takes no batch is refused`() {
        assertThrows<ObservesOnlyOnBatchMethods> { ReactorHandlers.from(ObservesOnPerEventReactor::class) }
    }

    @Test
    fun `a batch method observing nothing is refused`() {
        assertThrows<BatchMethodObservesNothing> { ReactorHandlers.from(ObservesNothingReactor::class) }
    }

    @Test
    fun `a batch method observing a class that is not an event type is refused`() {
        assertThrows<BatchMethodObservesUnknownEventType> { ReactorHandlers.from(ObservesNonEventReactor::class) }
    }

    @Test
    fun `an event type observed by two batch methods is refused`() {
        assertThrows<EventTypeObservedByMoreThanOneMethod> { ReactorHandlers.from(TwoBatchesOneTypeReactor::class) }
    }

    @Test
    fun `an event type observed by a batch method and handled per event is refused`() {
        assertThrows<EventTypeObservedByMoreThanOneMethod> { ReactorHandlers.from(BatchAndPerEventOneTypeReactor::class) }
    }

    @Test
    fun `a per-event reactor handler taking a batch context is refused`() {
        assertThrows<BatchContextOnlyForBatchMethods> { ReactorHandlers.from(PerEventTakingBatchContextReactor::class) }
    }

    @Test
    fun `a reactor batch method returning something is refused`() {
        assertThrows<InvalidBatchMethod> { ReactorHandlers.from(ReturningBatchReactor::class) }
    }

    @Test
    fun `a reactor batch method asking for something it cannot be given is refused`() {
        assertThrows<InvalidBatchMethod> { ReactorHandlers.from(UnsuppliableBatchParameterReactor::class) }
    }

    @Test
    fun `a replay batch method is refused`() {
        assertThrows<InvalidBatchMethod> { ReactorHandlers.from(ReplayBatchReactor::class) }
    }

    @Test
    fun `a batch reducer registers the event types it observes and infers its read model`() {
        val registration = ReducerRegistration.from(BatchReducer::class)

        assertEquals(setOf("AccountOpened", "MoneyDeposited"), registration.eventTypes.keys)
        assertEquals(AccountBalance::class, registration.readModelClass)
        assertTrue(registration.handlers.isEmpty())
        assertEquals(Runtime.getRuntime().availableProcessors(), registration.maxConcurrency)
    }

    @Test
    fun `a reducer can bound how many event sources it reduces at once`() {
        assertEquals(3, ReducerRegistration.from(BoundedBatchReducer::class).maxConcurrency)
    }

    @Test
    fun `a reducer with two batch methods is refused`() {
        assertThrows<ReducerHasMoreThanOneBatchMethod> { ReducerRegistration.from(TwoBatchReducer::class) }
    }

    @Test
    fun `a reducer mixing a batch method and per-event handlers is refused`() {
        assertThrows<ReducerMixesBatchAndPerEventMethods> { ReducerRegistration.from(MixedReducer::class) }
    }

    @Test
    fun `a reducer batch method not taking the read model it returns is refused`() {
        assertThrows<InvalidBatchMethod> { ReducerRegistration.from(WrongStateReducer::class) }
    }

    @Test
    fun `a per-event reducer handler taking a batch context is refused`() {
        assertThrows<BatchContextOnlyForBatchMethods> { ReducerRegistration.from(PerEventTakingBatchContextReducer::class) }
    }
}
