// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder

@Constraint
class UniqueEmployeeHire : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(EmployeeHired::class, "An employee can only be hired once.")
    }
}

@Constraint
class UniqueEmployeeEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(EmployeeEmailSet::class) { it.email }
                .ignoreCasing()
                .withMessage("That email address is already in use by another employee.")
        }
    }
}
