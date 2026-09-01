// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.artifacts.given.OrderClockArgument
import io.cratis.chronicle.artifacts.given.OrderList
import io.cratis.chronicle.artifacts.given.OrderListProjection
import io.cratis.chronicle.artifacts.given.OrderLogging
import io.cratis.chronicle.artifacts.given.OrderPlaced
import io.cratis.chronicle.artifacts.given.OrderReactor
import io.cratis.chronicle.artifacts.given.OrderSummary
import io.cratis.chronicle.artifacts.given.OrderTiming
import io.cratis.chronicle.artifacts.given.ShippingLabel
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.IReactorMiddleware
import io.cratis.chronicle.observation.ReadModelArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KnownClientArtifactsTests {

    /** A middleware that needs its enclosing instance to exist — nothing a scan can construct. */
    inner class InnerMiddleware : IReactorMiddleware {
        override suspend fun beforeInvoke(context: EventContext, event: Any) {}
        override suspend fun afterInvoke(context: EventContext, event: Any) {}
    }

    @Test
    fun `sorts each listed class into the kind it belongs to`() {
        val artifacts = KnownClientArtifacts(
            OrderPlaced::class,
            OrderReactor::class,
            OrderListProjection::class,
            OrderList::class
        )

        assertEquals(listOf(OrderPlaced::class), artifacts.eventTypes)
        assertEquals(listOf(OrderReactor::class), artifacts.reactors)
        assertEquals(listOf(OrderListProjection::class), artifacts.projections)
        assertEquals(listOf(OrderList::class), artifacts.readModels)
    }

    @Test
    fun `sorts the client-side kinds the same way the scan does`() {
        val artifacts = KnownClientArtifacts(
            OrderTiming::class,
            OrderLogging::class,
            OrderClockArgument::class
        )

        assertEquals(listOf(OrderTiming::class, OrderLogging::class), artifacts.reactorMiddlewares)
        assertEquals(listOf(OrderClockArgument::class), artifacts.reactorArgumentResolvers)
    }

    @Test
    fun `sorts a class into every kind it qualifies for`() {
        val artifacts = KnownClientArtifacts(OrderSummary::class)

        assertTrue(artifacts.readModels.contains(OrderSummary::class))
        assertTrue(artifacts.modelBoundProjections.contains(OrderSummary::class))
    }

    @Test
    fun `ignores a class that is not an artifact`() {
        val artifacts = KnownClientArtifacts(ShippingLabel::class)

        assertTrue(artifacts.eventTypes.isEmpty())
        assertTrue(artifacts.readModels.isEmpty())
        assertTrue(artifacts.reactors.isEmpty())
    }

    @Test
    fun `lists a class once even when it is given twice`() {
        val artifacts = KnownClientArtifacts(OrderPlaced::class, OrderPlaced::class)

        assertEquals(listOf(OrderPlaced::class), artifacts.eventTypes)
    }

    @Test
    fun `ignores a middleware written as an anonymous object`() {
        val anonymous = object : IReactorMiddleware {
            override suspend fun beforeInvoke(context: EventContext, event: Any) {}
            override suspend fun afterInvoke(context: EventContext, event: Any) {}
        }

        val artifacts = KnownClientArtifacts(anonymous::class)

        assertTrue(artifacts.reactorMiddlewares.isEmpty())
    }

    @Test
    fun `ignores a middleware declared inside a function`() {
        class Local : IReactorMiddleware {
            override suspend fun beforeInvoke(context: EventContext, event: Any) {}
            override suspend fun afterInvoke(context: EventContext, event: Any) {}
        }

        val artifacts = KnownClientArtifacts(Local::class)

        assertTrue(artifacts.reactorMiddlewares.isEmpty())
    }

    @Test
    fun `ignores a middleware declared as an inner class`() {
        val artifacts = KnownClientArtifacts(InnerMiddleware::class)

        assertTrue(artifacts.reactorMiddlewares.isEmpty())
    }

    @Test
    fun `ignores the argument resolvers the client installs itself`() {
        val artifacts = KnownClientArtifacts(ReadModelArgument::class)

        assertTrue(artifacts.reactorArgumentResolvers.isEmpty())
    }

    @Test
    fun `empty holds nothing`() {
        assertTrue(KnownClientArtifacts.empty.eventTypes.isEmpty())
        assertTrue(KnownClientArtifacts.empty.reactors.isEmpty())
        assertTrue(KnownClientArtifacts.empty.reducers.isEmpty())
    }
}
