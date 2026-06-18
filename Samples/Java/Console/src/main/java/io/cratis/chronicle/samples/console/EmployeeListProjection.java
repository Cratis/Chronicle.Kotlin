// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

import io.cratis.chronicle.java.ProjectionBuilderJavaBridge;

/** Declarative projection artifact discovered by the Kotlin client. */
@Projection
public class EmployeeListProjection implements IProjectionFor<Employee> {
    @Override
    public void define(IProjectionBuilderFor<Employee> builder) {
        ProjectionBuilderJavaBridge.from(builder, EmployeeHired.class);
        ProjectionBuilderJavaBridge.from(builder, EmployeeAddressSet.class);
        // EmployeePromoted updates title via auto-mapping (newTitle property in event → title in read model)
        ProjectionBuilderJavaBridge.from(builder, EmployeePromoted.class);
        ProjectionBuilderJavaBridge.from(builder, EmployeeMoved.class);
    }
}
