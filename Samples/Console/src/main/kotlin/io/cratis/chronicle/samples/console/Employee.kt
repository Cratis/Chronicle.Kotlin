// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

/**
 * Read model for the employee list, built as a model-bound projection.
 *
 * Each [FromEvent] annotation on the class declares which event types contribute to this read model.
 * Properties are auto-mapped by name unless overridden with [SetFrom].
 */
@ReadModel
@FromEvent(EmployeeHired::class)
@FromEvent(EmployeeAddressSet::class)
@FromEvent(EmployeeMoved::class)
@FromEvent(EmployeePromoted::class)
data class Employee(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    @SetFrom("newTitle", EmployeePromoted::class) val title: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = ""
)
