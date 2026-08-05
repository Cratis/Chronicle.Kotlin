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
        builder
            // Scope uniqueness checking to be per event source type rather than globally across
            // the whole event store — this keeps employee email uniqueness from ever colliding
            // with an unrelated event source type (e.g. customers) that also happens to set emails.
            .perEventSourceType()
            .unique { unique ->
                unique
                    .on(EmployeeEmailSet::class, EmployeeEmailSet::email)
                    .ignoreCasing()
                    .withMessage("That email address is already in use by another employee.")
            }
    }
}
