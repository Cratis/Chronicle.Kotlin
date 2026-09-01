// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer

/**
 * Folds employee events into [EmployeeState].
 *
 * Every handler takes the [EventContext] as a third parameter and stamps `id` from its event source
 * id. That is not decoration: the sink stores the read model under its `id`, so leaving it empty
 * puts every employee on the same key and they overwrite one another.
 */
@Reducer
class EmployeeStateReducer {

    fun employeeHired(event: EmployeeHired, state: EmployeeState?, context: EventContext): EmployeeState {
        println("[reducer] EmployeeHired: ${event.firstName} ${event.lastName}")
        return (state ?: EmployeeState()).copy(
            id = context.eventSourceId,
            firstName = event.firstName, lastName = event.lastName, title = event.title
        )
    }

    fun employeeAddressSet(event: EmployeeAddressSet, state: EmployeeState?, context: EventContext): EmployeeState {
        println("[reducer] EmployeeAddressSet: ${event.city}")
        return (state ?: EmployeeState()).copy(
            id = context.eventSourceId,
            address = event.address, city = event.city,
            zipCode = event.zipCode, country = event.country
        )
    }

    fun employeeEmailSet(event: EmployeeEmailSet, state: EmployeeState?, context: EventContext): EmployeeState {
        println("[reducer] EmployeeEmailSet: ${event.email}")
        return (state ?: EmployeeState()).copy(id = context.eventSourceId, email = event.email)
    }

    fun employeePromoted(event: EmployeePromoted, state: EmployeeState?, context: EventContext): EmployeeState {
        println("[reducer] EmployeePromoted: ${event.newTitle}")
        return (state ?: EmployeeState()).copy(id = context.eventSourceId, title = event.newTitle)
    }

    fun employeeMoved(event: EmployeeMoved, state: EmployeeState?, context: EventContext): EmployeeState {
        println("[reducer] EmployeeMoved: ${event.city}")
        return (state ?: EmployeeState()).copy(
            id = context.eventSourceId,
            address = event.address, city = event.city,
            zipCode = event.zipCode, country = event.country
        )
    }
}
