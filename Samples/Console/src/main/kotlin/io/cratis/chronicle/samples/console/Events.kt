// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.events.EventType

/** An employee has been hired into the organization. */
@EventType
data class EmployeeHired(
    val firstName: String = "",
    val lastName: String = "",
    val title: String = ""
)

/** An employee's address has been set. */
@EventType
data class EmployeeAddressSet(
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = ""
)

/** An employee has been promoted to a new title. */
@EventType
data class EmployeePromoted(val newTitle: String = "")

/** An employee's email address has been set. */
@EventType
data class EmployeeEmailSet(val email: String = "")

/** An employee has relocated to a new address. */
@EventType
data class EmployeeMoved(
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = ""
)
