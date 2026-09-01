// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reducer;

/**
 * Folds employee events into {@link EmployeeState}.
 * <p>
 * Every handler takes the {@link EventContext} as a third parameter and stamps {@code id} from its
 * event source id. That is not decoration: the sink stores the read model under its {@code id}, so
 * leaving it empty puts every employee on the same key and they overwrite one another.
 */
@Reducer
public class EmployeeStateReducer {

    public EmployeeState employeeHired(EmployeeHired event, EmployeeState state, EventContext context) {
        System.out.println("[reducer] EmployeeHired: " + event.firstName() + " " + event.lastName());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            event.firstName(),
            event.lastName(),
            event.title(),
            current.getEmail(),
            current.getAddress(),
            current.getCity(),
            current.getZipCode(),
            current.getCountry()
        );
    }

    public EmployeeState employeeAddressSet(EmployeeAddressSet event, EmployeeState state, EventContext context) {
        System.out.println("[reducer] EmployeeAddressSet: " + event.city());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            current.getFirstName(),
            current.getLastName(),
            current.getTitle(),
            current.getEmail(),
            event.address(),
            event.city(),
            event.zipCode(),
            event.country()
        );
    }

    public EmployeeState employeeEmailSet(EmployeeEmailSet event, EmployeeState state, EventContext context) {
        System.out.println("[reducer] EmployeeEmailSet: " + event.email());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            current.getFirstName(),
            current.getLastName(),
            current.getTitle(),
            event.email(),
            current.getAddress(),
            current.getCity(),
            current.getZipCode(),
            current.getCountry()
        );
    }

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state, EventContext context) {
        System.out.println("[reducer] EmployeePromoted: " + event.newTitle());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            current.getFirstName(),
            current.getLastName(),
            event.newTitle(),
            current.getEmail(),
            current.getAddress(),
            current.getCity(),
            current.getZipCode(),
            current.getCountry()
        );
    }

    public EmployeeState employeeMoved(EmployeeMoved event, EmployeeState state, EventContext context) {
        System.out.println("[reducer] EmployeeMoved: " + event.city());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            context.getEventSourceId(),
            current.getFirstName(),
            current.getLastName(),
            current.getTitle(),
            current.getEmail(),
            event.address(),
            event.city(),
            event.zipCode(),
            event.country()
        );
    }
}
