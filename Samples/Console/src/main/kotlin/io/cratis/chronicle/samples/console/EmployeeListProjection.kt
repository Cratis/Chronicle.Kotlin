// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@Projection(readModel = Employee::class)
class EmployeeListProjection : IProjectionFor<Employee> {
    override fun define(builder: IProjectionBuilderFor<Employee>) {
        builder
            .from(EmployeeHired::class)
            .from(EmployeeAddressSet::class)
            .from(EmployeePromoted::class) { fb ->
                fb.set(Employee::title).toProperty("newTitle")
            }
            .from(EmployeeMoved::class)
    }
}
