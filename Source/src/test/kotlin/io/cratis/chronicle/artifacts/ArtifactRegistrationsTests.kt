// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.artifacts.given.OrderArchive
import io.cratis.chronicle.artifacts.given.OrderList
import io.cratis.chronicle.artifacts.given.OrderListProjection
import io.cratis.chronicle.artifacts.given.OrderPlaced
import io.cratis.chronicle.artifacts.given.OrderReactor
import io.cratis.chronicle.artifacts.given.OrderSeeder
import io.cratis.chronicle.artifacts.given.OrderShipped
import io.cratis.chronicle.artifacts.given.OrderShippedMigration
import io.cratis.chronicle.artifacts.given.OrderShippedV1
import io.cratis.chronicle.artifacts.given.OrderState
import io.cratis.chronicle.artifacts.given.OrderStateReducer
import io.cratis.chronicle.artifacts.given.OrderSummary
import io.cratis.chronicle.artifacts.given.OrderWebhook
import io.cratis.chronicle.artifacts.given.UniqueOrderNumber
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.webhooks.IWebhooksService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ArtifactRegistrationsTests {

    /** Records what was registered, and in which order, so both can be asserted without mocking gymnastics. */
    private class Recorder {
        val order = mutableListOf<String>()
        val registered = mutableMapOf<String, MutableList<Any>>()

        fun record(what: String, arguments: Array<*>) {
            order += what
            registered.getOrPut(what) { mutableListOf() }.addAll(arguments.filterNotNull())
        }

        fun of(what: String): List<Any> = registered[what].orEmpty()

        fun timesCalled(what: String): Int = order.count { it == what }
    }

    private val recorder = Recorder()
    private val eventTypes = mockk<IEventTypesService>(relaxed = true)
    private val readModels = mockk<IReadModelsService>(relaxed = true)
    private val constraints = mockk<IConstraintsService>(relaxed = true)
    private val projections = mockk<IProjectionsService>(relaxed = true)
    private val webhooks = mockk<IWebhooksService>(relaxed = true)
    private val reactors = mockk<IReactorsService>(relaxed = true)
    private val reducers = mockk<IReducersService>(relaxed = true)
    private val seeding = mockk<IEventSeedingService>(relaxed = true)
    private val eventStore = mockk<IEventStore>(relaxed = true)

    private val artifacts = ClientArtifacts("io.cratis.chronicle.artifacts.given")

    init {
        every { eventStore.eventTypes } returns eventTypes
        every { eventStore.readModels } returns readModels
        every { eventStore.constraints } returns constraints
        every { eventStore.projections } returns projections
        every { eventStore.webhooks } returns webhooks
        every { eventStore.reactors } returns reactors
        every { eventStore.reducers } returns reducers
        every { eventStore.seeding } returns seeding

        coEvery { eventTypes.register(*anyVararg()) } answers { recorder.record(EVENT_TYPES, firstArg()) }
        coEvery { readModels.register(*anyVararg()) } answers { recorder.record(READ_MODELS, firstArg()) }
        coEvery { constraints.register(*anyVararg()) } answers { recorder.record(CONSTRAINTS, firstArg()) }
        coEvery { projections.register(*anyVararg()) } answers { recorder.record(PROJECTIONS, firstArg()) }
        coEvery { webhooks.register(*anyVararg()) } answers { recorder.record(WEBHOOKS, firstArg()) }
        coEvery { seeding.seed(*anyVararg()) } answers { recorder.record(SEEDERS, firstArg()) }
        coEvery { reactors.register(any()) } answers {
            recorder.record(REACTORS, arrayOf(firstArg<Any>()))
            Job()
        }
        coEvery { reducers.register(any()) } answers {
            recorder.record(REDUCERS, arrayOf(firstArg<Any>()))
            Job()
        }
    }

    private fun registrations(activator: IArtifactActivator = ArtifactActivator) =
        ArtifactRegistrations(eventStore, artifacts, activator)

    @Test
    fun `registers event types before anything that refers to them`() = runTest {
        registrations().registerAll()

        assertTrue(recorder.order.indexOf(EVENT_TYPES) < recorder.order.indexOf(PROJECTIONS))
        assertTrue(recorder.order.indexOf(EVENT_TYPES) < recorder.order.indexOf(CONSTRAINTS))
        assertTrue(recorder.order.indexOf(EVENT_TYPES) < recorder.order.indexOf(REACTORS))
    }

    @Test
    fun `seeds only once every observer is watching`() = runTest {
        registrations().registerAll()

        assertTrue(recorder.order.indexOf(REACTORS) < recorder.order.indexOf(SEEDERS))
        assertTrue(recorder.order.indexOf(REDUCERS) < recorder.order.indexOf(SEEDERS))
        assertTrue(recorder.order.indexOf(PROJECTIONS) < recorder.order.indexOf(SEEDERS))
    }

    @Test
    fun `registers event types and their migrations in one call so generations are merged`() = runTest {
        registrations().registerAll()

        val registered = recorder.of(EVENT_TYPES)
        assertTrue(registered.contains(OrderPlaced::class))
        assertTrue(registered.contains(OrderShipped::class))
        assertTrue(registered.contains(OrderShippedV1::class))
        assertTrue(registered.contains(OrderShippedMigration::class))
    }

    @Test
    fun `registers the read models no observer produces`() = runTest {
        registrations().registerAll()

        assertTrue(recorder.of(READ_MODELS).contains(OrderArchive::class))
    }

    @Test
    fun `leaves a read model produced by a reducer to that reducer`() = runTest {
        registrations().registerAll()

        assertFalse(recorder.of(READ_MODELS).contains(OrderState::class))
    }

    @Test
    fun `leaves a read model produced by a projection to that projection`() = runTest {
        registrations().registerAll()

        assertFalse(recorder.of(READ_MODELS).contains(OrderList::class))
        assertFalse(recorder.of(READ_MODELS).contains(OrderSummary::class))
    }

    @Test
    fun `registers declarative and model-bound projections together`() = runTest {
        registrations().registerAll()

        val registered = recorder.of(PROJECTIONS)
        assertTrue(registered.any { it is OrderListProjection })
        assertTrue(registered.contains(OrderSummary::class))
    }

    @Test
    fun `registers constraints, webhooks and seeders as activated instances`() = runTest {
        registrations().registerAll()

        assertTrue(recorder.of(CONSTRAINTS).any { it is UniqueOrderNumber })
        assertTrue(recorder.of(WEBHOOKS).any { it is OrderWebhook })
        assertTrue(recorder.of(SEEDERS).any { it is OrderSeeder })
    }

    @Test
    fun `starts an observation for every reactor and reducer`() = runTest {
        registrations().registerAll()

        assertTrue(recorder.of(REACTORS).any { it is OrderReactor })
        assertTrue(recorder.of(REDUCERS).any { it is OrderStateReducer })
    }

    @Test
    fun `states the declarations again on a second pass`() = runTest {
        val subject = registrations()
        subject.registerAll()
        subject.registerAll()

        assertEquals(2, recorder.timesCalled(EVENT_TYPES))
        assertEquals(2, recorder.timesCalled(PROJECTIONS))
    }

    @Test
    fun `never starts a second observation for the same reactor or reducer`() = runTest {
        val subject = registrations()
        subject.registerAll()
        subject.registerAll()

        assertEquals(1, recorder.timesCalled(REACTORS))
        assertEquals(1, recorder.timesCalled(REDUCERS))
    }

    @Test
    fun `reports completion once the first pass is through`() = runTest {
        val subject = registrations()
        assertFalse(subject.completed.isCompleted)

        subject.registerAll()

        assertTrue(subject.completed.isCompleted)
    }

    @Test
    fun `activates every artifact through the configured activator`() = runTest {
        val activated = mutableListOf<KClass<*>>()

        registrations { type -> activated += type; ArtifactActivator.activate(type) }.registerAll()

        assertTrue(activated.contains(OrderReactor::class))
        assertTrue(activated.contains(OrderStateReducer::class))
        assertTrue(activated.contains(UniqueOrderNumber::class))
        assertTrue(activated.contains(OrderSeeder::class))
        assertTrue(activated.contains(OrderWebhook::class))
        assertTrue(activated.contains(OrderListProjection::class))
    }

    private companion object {
        const val EVENT_TYPES = "eventTypes"
        const val READ_MODELS = "readModels"
        const val CONSTRAINTS = "constraints"
        const val PROJECTIONS = "projections"
        const val WEBHOOKS = "webhooks"
        const val REACTORS = "reactors"
        const val REDUCERS = "reducers"
        const val SEEDERS = "seeders"
    }
}
