// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.projections.NotRewindable
import io.cratis.chronicle.readModels.ReadModel

/** The name of the (hypothetical) external event store the payroll system owns. */
private const val payrollEventStoreName = "PayrollEventStore"

/** The identifier of this store's subscription to the payroll system's outbox. */
private const val payrollSubscriptionId = "payroll-inbox"

/**
 * A payroll run reported by the external payroll system. Owned by [payrollEventStoreName] — this
 * store only ever sees it through the [payrollSubscriptionId] subscription's inbox, but it still
 * needs to be registered locally so this store knows the event's shape.
 */
@EventType
data class PayrollRunCompleted(val employeeId: String = "", val amount: Double = 0.0)

/**
 * A read model over payroll runs ingested from the external payroll system.
 *
 * [PayrollRunCompleted] only carries the employee's id, not their name, so [employeeFirstName]
 * and [employeeLastName] are pulled in with [Join] against this store's own [EmployeeHired] event,
 * correlated on the read model's own `id` (which equals the employee id here).
 *
 * Marked [NotRewindable] because a rewind would require the payroll system to redeliver events
 * through its outbox, which it may no longer have available.
 */
@ReadModel
@FromEvent(PayrollRunCompleted::class)
@NotRewindable
data class PayrollRunSummary(
    val id: String = "",
    val amount: Double = 0.0,
    @Join(eventType = EmployeeHired::class, on = "id", eventPropertyName = "firstName") val employeeFirstName: String = "",
    @Join(eventType = EmployeeHired::class, on = "id", eventPropertyName = "lastName") val employeeLastName: String = ""
)

/**
 * Subscribes this event store to [payrollEventStoreName]'s outbox for [PayrollRunCompleted] events,
 * and registers the [PayrollRunSummary] projection that consumes them.
 */
suspend fun setupPayrollIntegration(store: EventStore) {
    store.eventStoreSubscriptions.subscribe(payrollSubscriptionId, payrollEventStoreName) { builder ->
        builder.withEventType(PayrollRunCompleted::class)
    }
    store.projections.register(PayrollRunSummary::class)
    println("[subscriptions] Subscribed '$payrollSubscriptionId' to '$payrollEventStoreName' for PayrollRunCompleted events.")
}

/** Lists the event store subscriptions currently registered for this event store. */
suspend fun listEventStoreSubscriptions(store: EventStore) {
    val subscriptions = store.eventStoreSubscriptions.getAll()
    if (subscriptions.isEmpty()) {
        println("[subscriptions] No event store subscriptions registered.")
        return
    }
    println("[subscriptions] ${subscriptions.size} subscription(s):")
    subscriptions.forEach { subscription ->
        println("  ${subscription.identifier} <- ${subscription.sourceEventStore} (${subscription.eventTypesList.size} event type filter(s))")
    }
}
