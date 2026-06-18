// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.observation.Reducer

@Reducer
class EmployeeStateReducer {

    fun employeeHired(event: EmployeeHired): EmployeeState {
        println("[reducer] EmployeeHired: ${event.firstName} ${event.lastName}")
        return EmployeeState(firstName = event.firstName, lastName = event.lastName, title = event.title)
    }

    fun employeeAddressSet(event: EmployeeAddressSet, state: EmployeeState?): EmployeeState {
        println("[reducer] EmployeeAddressSet: ${event.city}")
        return (state ?: EmployeeState()).copy(
            address = event.address, city = event.city,
            zipCode = event.zipCode, country = event.country
        )
    }

    fun employeeEmailSet(event: EmployeeEmailSet, state: EmployeeState?): EmployeeState {
        println("[reducer] EmployeeEmailSet: ${event.email}")
        return (state ?: EmployeeState()).copy(email = event.email)
    }

    fun employeePromoted(event: EmployeePromoted, state: EmployeeState?): EmployeeState {
        println("[reducer] EmployeePromoted: ${event.newTitle}")
        return (state ?: EmployeeState()).copy(title = event.newTitle)
    }

    fun employeeMoved(event: EmployeeMoved, state: EmployeeState?): EmployeeState {
        println("[reducer] EmployeeMoved: ${event.city}")
        return (state ?: EmployeeState()).copy(
            address = event.address, city = event.city,
            zipCode = event.zipCode, country = event.country
        )
    }
}
