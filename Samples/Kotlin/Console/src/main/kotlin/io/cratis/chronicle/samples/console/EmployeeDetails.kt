// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEventSourceId
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

/**
 * Model-bound projection artifact discovered by the Kotlin client.
 *
 * Each [FromEvent] annotation on the class declares which event types contribute to this read model.
 * Properties are auto-mapped by name unless overridden with [SetFrom].
 */
@ReadModel
@FromEvent(EmployeeHired::class)
@FromEvent(EmployeeAddressSet::class)
@FromEvent(EmployeePromoted::class)
@FromEvent(EmployeeMoved::class)
data class EmployeeDetails(
    /**
     * The read model's key, taken from the event source id of every event it observes.
     *
     * The sink stores each instance under its `id`, so a projection whose read model has one has to
     * say where it comes from - leave it empty and every employee lands on the same document.
     *
     * The kernel does not act on this yet (https://github.com/Cratis/Chronicle/issues/3924), which
     * is why pressing 'L' lists one merged employee rather than three. The reducer-backed
     * EmployeeState next door stamps its own key and does show all three.
     */
    @FromEventSourceId val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    @SetFrom("newTitle", EmployeePromoted::class) val title: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "",
    /** Bumped by one every time this employee is promoted — demonstrates [Increment]. */
    @Increment(EmployeePromoted::class) val promotionCount: Int = 0
)
