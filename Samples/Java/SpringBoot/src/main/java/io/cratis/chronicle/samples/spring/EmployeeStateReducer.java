// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.observation.Reducer;

/** Folds employee events into {@link EmployeeState}. Discovered and registered by the starter — no wiring needed. */
@Reducer
public class EmployeeStateReducer {
    public EmployeeState employeeHired(EmployeeHired event) {
        return new EmployeeState("", event.firstName(), event.lastName(), event.title(), "");
    }

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state) {
        EmployeeState current = state != null ? state : new EmployeeState();
        current.setTitle(event.newTitle());
        return current;
    }

    public EmployeeState employeeEmailSet(EmployeeEmailSet event, EmployeeState state) {
        EmployeeState current = state != null ? state : new EmployeeState();
        current.setEmail(event.email());
        return current;
    }
}
