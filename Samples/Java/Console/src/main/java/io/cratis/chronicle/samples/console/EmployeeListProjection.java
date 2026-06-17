// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

/** Declarative projection artifact discovered by the Kotlin client. */
@Projection
public class EmployeeListProjection implements IProjectionFor<Employee> {
    @Override
    public void define(IProjectionBuilderFor<Employee> builder) {
        builder
            .from(EmployeeHired.class)
            .from(EmployeeAddressSet.class)
            .from(EmployeePromoted.class, fb -> 
                fb.set(Employee::getTitle).toProperty("newTitle")
            )
            .from(EmployeeMoved.class);
    }
}
