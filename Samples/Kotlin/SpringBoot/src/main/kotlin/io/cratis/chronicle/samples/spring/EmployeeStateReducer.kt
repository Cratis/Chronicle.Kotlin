// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer

/**
 * Folds employee events into [EmployeeState]. Discovered and registered by the starter — no wiring needed.
 *
 * Every handler takes the [EventContext] as a third parameter and stamps `id` from its event source
 * id. The sink stores the read model under its `id`, so leaving it empty puts every employee on the
 * same key and they overwrite one another.
 */
@Reducer
class EmployeeStateReducer {
    fun employeeHired(event: EmployeeHired, state: EmployeeState?, context: EventContext): EmployeeState =
        (state ?: EmployeeState()).copy(
            id = context.eventSourceId,
            firstName = event.firstName, lastName = event.lastName, title = event.title
        )

    fun employeePromoted(event: EmployeePromoted, state: EmployeeState?, context: EventContext): EmployeeState =
        (state ?: EmployeeState()).copy(id = context.eventSourceId, title = event.newTitle)

    fun employeeEmailSet(event: EmployeeEmailSet, state: EmployeeState?, context: EventContext): EmployeeState =
        (state ?: EmployeeState()).copy(id = context.eventSourceId, email = event.email)
}
