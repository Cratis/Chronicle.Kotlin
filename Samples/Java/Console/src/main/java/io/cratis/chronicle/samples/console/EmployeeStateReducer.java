// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.observation.Reducer;

@Reducer
public class EmployeeStateReducer {

    public EmployeeState employeeHired(EmployeeHired event) {
        System.out.println("[reducer] EmployeeHired: " + event.firstName() + " " + event.lastName());
        return new EmployeeState("", event.firstName(), event.lastName(), event.title(), 
                                "", "", "", "", "");
    }

    public EmployeeState employeeAddressSet(EmployeeAddressSet event, EmployeeState state) {
        System.out.println("[reducer] EmployeeAddressSet: " + event.city());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            current.getId(),
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

    public EmployeeState employeeEmailSet(EmployeeEmailSet event, EmployeeState state) {
        System.out.println("[reducer] EmployeeEmailSet: " + event.email());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            current.getId(),
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

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state) {
        System.out.println("[reducer] EmployeePromoted: " + event.newTitle());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            current.getId(),
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

    public EmployeeState employeeMoved(EmployeeMoved event, EmployeeState state) {
        System.out.println("[reducer] EmployeeMoved: " + event.city());
        EmployeeState current = state != null ? state : new EmployeeState();
        return new EmployeeState(
            current.getId(),
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
