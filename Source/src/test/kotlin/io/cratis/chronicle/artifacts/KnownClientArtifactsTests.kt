// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.artifacts.given.OrderList
import io.cratis.chronicle.artifacts.given.OrderListProjection
import io.cratis.chronicle.artifacts.given.OrderPlaced
import io.cratis.chronicle.artifacts.given.OrderReactor
import io.cratis.chronicle.artifacts.given.OrderSummary
import io.cratis.chronicle.artifacts.given.ShippingLabel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KnownClientArtifactsTests {

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
    fun `empty holds nothing`() {
        assertTrue(KnownClientArtifacts.empty.eventTypes.isEmpty())
        assertTrue(KnownClientArtifacts.empty.reactors.isEmpty())
        assertTrue(KnownClientArtifacts.empty.reducers.isEmpty())
    }
}
