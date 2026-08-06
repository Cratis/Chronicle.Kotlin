// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.events.EventType

/** An employee has been hired into the organization. */
@EventType
data class EmployeeHired(
    val firstName: String = "",
    val lastName: String = "",
    val title: String = ""
)

/** An employee has been promoted to a new title. */
@EventType
data class EmployeePromoted(val newTitle: String = "")

/** An employee's email address has been set. */
@EventType
data class EmployeeEmailSet(val email: String = "")

/** A welcome package has been requested for a newly hired employee. */
@EventType
data class WelcomePackageRequested(val employeeId: String = "")
