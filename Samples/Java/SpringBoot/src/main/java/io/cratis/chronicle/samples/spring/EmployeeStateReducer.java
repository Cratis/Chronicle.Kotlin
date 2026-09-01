// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reducer;

/**
 * Folds employee events into {@link EmployeeState}. Discovered and registered by the starter — no wiring needed.
 * <p>
 * Every handler takes the {@link EventContext} as a third parameter and stamps {@code id} from its
 * event source id. The sink stores the read model under its {@code id}, so leaving it empty puts
 * every employee on the same key and they overwrite one another.
 */
@Reducer
public class EmployeeStateReducer {
    public EmployeeState employeeHired(EmployeeHired event, EmployeeState state, EventContext context) {
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            event.firstName(),
            event.lastName(),
            event.title(),
            current.getEmail());
    }

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state, EventContext context) {
        EmployeeState current = state != null ? state : new EmployeeState();
        current.setId(context.getEventSourceId());
        current.setTitle(event.newTitle());
        return current;
    }

    public EmployeeState employeeEmailSet(EmployeeEmailSet event, EmployeeState state, EventContext context) {
        EmployeeState current = state != null ? state : new EmployeeState();
        current.setId(context.getEventSourceId());
        current.setEmail(event.email());
        return current;
    }
}
