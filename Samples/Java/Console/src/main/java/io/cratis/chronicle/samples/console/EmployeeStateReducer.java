// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.observation.Reducer;

@Reducer
public class EmployeeStateReducer {

    public EmployeeState employeeHired(EmployeeHired event) {
        System.out.println("[reducer] EmployeeHired: " + event.getFirstName() + " " + event.getLastName());
        return new EmployeeState("", event.getFirstName(), event.getLastName(), event.getTitle(), 
                                "", "", "", "", "");
    }

    public EmployeeState employeeAddressSet(EmployeeAddressSet event, EmployeeState state) {
        System.out.println("[reducer] EmployeeAddressSet: " + event.getCity());
        EmployeeState result = state != null ? state : new EmployeeState();
        result.setAddress(event.getAddress());
        result.setCity(event.getCity());
        result.setZipCode(event.getZipCode());
        result.setCountry(event.getCountry());
        return result;
    }

    public EmployeeState employeeEmailSet(EmployeeEmailSet event, EmployeeState state) {
        System.out.println("[reducer] EmployeeEmailSet: " + event.getEmail());
        EmployeeState result = state != null ? state : new EmployeeState();
        result.setEmail(event.getEmail());
        return result;
    }

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state) {
        System.out.println("[reducer] EmployeePromoted: " + event.getNewTitle());
        EmployeeState result = state != null ? state : new EmployeeState();
        result.setTitle(event.getNewTitle());
        return result;
    }

    public EmployeeState employeeMoved(EmployeeMoved event, EmployeeState state) {
        System.out.println("[reducer] EmployeeMoved: " + event.getCity());
        EmployeeState result = state != null ? state : new EmployeeState();
        result.setAddress(event.getAddress());
        result.setCity(event.getCity());
        result.setZipCode(event.getZipCode());
        result.setCountry(event.getCountry());
        return result;
    }
}
