// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.observation.Reducer

/** Folds employee events into [EmployeeState]. Discovered and registered by the starter — no wiring needed. */
@Reducer
class EmployeeStateReducer {
    fun employeeHired(event: EmployeeHired): EmployeeState =
        EmployeeState(firstName = event.firstName, lastName = event.lastName, title = event.title)

    fun employeePromoted(event: EmployeePromoted, state: EmployeeState?): EmployeeState =
        (state ?: EmployeeState()).copy(title = event.newTitle)

    fun employeeEmailSet(event: EmployeeEmailSet, state: EmployeeState?): EmployeeState =
        (state ?: EmployeeState()).copy(email = event.email)
}
