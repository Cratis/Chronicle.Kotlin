// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;

import io.cratis.chronicle.java.ConstraintBuilderJavaBridge;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@Constraint
class UniqueEmployeeHire implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        ConstraintBuilderJavaBridge.uniqueFor(builder, EmployeeHired.class, "An employee can only be hired once.");
    }
}

@Constraint
class UniqueEmployeeEmail implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, EmployeeEmailSet.class, EmployeeEmailSet::email)
                .ignoreCasing()
                .withMessage("That email address is already in use by another employee.");
            return null; // Java lambda returning Unit
        });
    }
}
