// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts.given

import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.IEventTypeMigration
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.java.BlockingReactorMiddleware
import io.cratis.chronicle.observation.IReactorMethodArgumentResolver
import io.cratis.chronicle.observation.IReactorMiddleware
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.webhooks.IWebhookDefiner
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * One of every kind of artifact, in a package of its own, so discovery can be exercised against a
 * known set rather than against whatever else happens to be on the test classpath.
 */

@EventType
data class OrderPlaced(val orderId: String = "", val customer: String = "")

@EventType(generation = 2)
data class OrderShipped(val orderId: String = "", val carrier: String = "")

@EventType
data class OrderShippedV1(val orderId: String = "")

/** Migrates [OrderShippedV1] forward to [OrderShipped]. */
class OrderShippedMigration : IEventTypeMigration<OrderShipped, OrderShippedV1> {
    override val targetClass: KClass<OrderShipped> = OrderShipped::class
    override val sourceClass: KClass<OrderShippedV1> = OrderShippedV1::class
}

/** A read model produced by [OrderStateReducer]. */
@ReadModel
data class OrderState(val id: String = "", val customer: String = "")

/** A read model produced by [OrderListProjection]. */
@ReadModel
data class OrderList(val id: String = "", val customer: String = "")

/** A read model produced by nothing at all — the client has to register it on its own. */
@ReadModel
data class OrderArchive(val id: String = "")

/** A read model that declares the events it projects from, rather than delegating to a projection class. */
@ReadModel
@FromEvent(OrderPlaced::class)
@FromEvent(OrderShipped::class)
data class OrderSummary(val id: String = "", val customer: String = "")

@Reducer
class OrderStateReducer {
    fun orderPlaced(event: OrderPlaced): OrderState = OrderState(customer = event.customer)
}

@Reactor
class OrderReactor {
    fun orderPlaced(event: OrderPlaced) = Unit
}

class OrderListProjection : IProjectionFor<OrderList> {
    override fun define(builder: IProjectionBuilderFor<OrderList>) {
        builder.from(OrderPlaced::class)
    }
}

@Constraint
class UniqueOrderNumber : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(OrderPlaced::class, "An order can only be placed once.")
    }
}

class OrderSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource("order-1", listOf(OrderPlaced("order-1", "Acme")))
    }
}

class OrderWebhook : IWebhookDefiner {
    override fun define(builder: IWebhookDefinitionBuilder) {
        builder.withEventType(OrderPlaced::class)
    }
}

/** Wraps every reactor handler invocation. Client-side only - never declared to the kernel. */
class OrderTiming : IReactorMiddleware {
    override suspend fun beforeInvoke(context: EventContext, event: Any) = Unit
    override suspend fun afterInvoke(context: EventContext, event: Any) = Unit
}

/** The same, written the way Java has to write it. */
class OrderLogging : BlockingReactorMiddleware {
    override fun beforeInvoke(context: EventContext, event: Any) = Unit
    override fun afterInvoke(context: EventContext, event: Any) = Unit
}

/** Supplies a reactor handler parameter the client knows nothing about. */
class OrderClockArgument : IReactorMethodArgumentResolver {
    override fun canResolve(parameter: KParameter): Boolean = false
    override suspend fun resolve(parameter: KParameter, context: EventContext): Any? = null
}

/** Not an artifact of any kind — proof that discovery does not simply sweep up everything in the package. */
data class ShippingLabel(val code: String = "")
