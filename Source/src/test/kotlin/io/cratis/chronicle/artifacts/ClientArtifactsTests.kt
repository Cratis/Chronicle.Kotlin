// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.artifacts.given.OrderArchive
import io.cratis.chronicle.artifacts.given.OrderClockArgument
import io.cratis.chronicle.artifacts.given.OrderList
import io.cratis.chronicle.artifacts.given.OrderListProjection
import io.cratis.chronicle.artifacts.given.OrderLogging
import io.cratis.chronicle.artifacts.given.OrderPlaced
import io.cratis.chronicle.artifacts.given.OrderReactor
import io.cratis.chronicle.artifacts.given.OrderSeeder
import io.cratis.chronicle.artifacts.given.OrderShipped
import io.cratis.chronicle.artifacts.given.OrderShippedMigration
import io.cratis.chronicle.artifacts.given.OrderState
import io.cratis.chronicle.artifacts.given.OrderStateReducer
import io.cratis.chronicle.artifacts.given.OrderSummary
import io.cratis.chronicle.artifacts.given.OrderTiming
import io.cratis.chronicle.artifacts.given.OrderWebhook
import io.cratis.chronicle.artifacts.given.ShippingLabel
import io.cratis.chronicle.artifacts.given.UniqueOrderNumber
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.projections.IProjectionFor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClientArtifactsTests {

    private val artifacts = ClientArtifacts("io.cratis.chronicle.artifacts.given")

    @Test
    fun `discovers every event type in the scanned packages`() {
        assertTrue(artifacts.eventTypes.contains(OrderPlaced::class))
        assertTrue(artifacts.eventTypes.contains(OrderShipped::class))
    }

    @Test
    fun `discovers event type migrations`() {
        assertTrue(artifacts.eventTypeMigrations.contains(OrderShippedMigration::class))
    }

    @Test
    fun `discovers read models`() {
        assertTrue(artifacts.readModels.contains(OrderState::class))
        assertTrue(artifacts.readModels.contains(OrderList::class))
        assertTrue(artifacts.readModels.contains(OrderArchive::class))
    }

    @Test
    fun `discovers declarative projections`() {
        assertTrue(artifacts.projections.contains(OrderListProjection::class))
    }

    @Test
    fun `discovers a read model carrying repeated from-event annotations as a model-bound projection`() {
        assertTrue(artifacts.modelBoundProjections.contains(OrderSummary::class))
    }

    @Test
    fun `discovers reactors`() {
        assertTrue(artifacts.reactors.contains(OrderReactor::class))
    }

    @Test
    fun `discovers reducers`() {
        assertTrue(artifacts.reducers.contains(OrderStateReducer::class))
    }

    @Test
    fun `discovers constraints`() {
        assertTrue(artifacts.constraints.contains(UniqueOrderNumber::class))
    }

    @Test
    fun `discovers event seeders`() {
        assertTrue(artifacts.eventSeeders.contains(OrderSeeder::class))
    }

    @Test
    fun `discovers webhook definers`() {
        assertTrue(artifacts.webhooks.contains(OrderWebhook::class))
    }

    @Test
    fun `discovers reactor middlewares written in kotlin and in java`() {
        assertTrue(artifacts.reactorMiddlewares.contains(OrderTiming::class))
        assertTrue(artifacts.reactorMiddlewares.contains(OrderLogging::class))
    }

    @Test
    fun `discovers reactor method argument resolvers`() {
        assertTrue(artifacts.reactorArgumentResolvers.contains(OrderClockArgument::class))
    }

    @Test
    fun `leaves classes that are not artifacts alone`() {
        val everything = artifacts.eventTypes + artifacts.readModels + artifacts.projections +
            artifacts.modelBoundProjections + artifacts.reactors + artifacts.reducers +
            artifacts.constraints + artifacts.eventSeeders + artifacts.webhooks +
            artifacts.eventTypeMigrations + artifacts.reactorMiddlewares +
            artifacts.reactorArgumentResolvers
        assertFalse(everything.contains(ShippingLabel::class))
    }

    @Test
    fun `leaves the contracts an artifact implements out of the discovered set`() {
        assertFalse(artifacts.constraints.contains(IConstraint::class))
        assertFalse(artifacts.projections.contains(IProjectionFor::class))
    }

    @Test
    fun `finds nothing outside the scanned packages`() {
        val elsewhere = ClientArtifacts("io.cratis.chronicle.artifacts.nothing.here")
        assertTrue(elsewhere.eventTypes.isEmpty())
        assertTrue(elsewhere.reactors.isEmpty())
    }
}
