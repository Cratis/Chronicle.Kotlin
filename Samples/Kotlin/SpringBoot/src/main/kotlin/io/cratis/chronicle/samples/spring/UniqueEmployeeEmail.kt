// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder

/** No two employees may hold the same email address — enforced by the kernel, not by a lookup before appending. */
@Constraint
class UniqueEmployeeEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(EmployeeEmailSet::class, EmployeeEmailSet::email)
                .ignoreCasing()
                .withMessage("That email address is already in use by another employee.")
        }
    }
}
