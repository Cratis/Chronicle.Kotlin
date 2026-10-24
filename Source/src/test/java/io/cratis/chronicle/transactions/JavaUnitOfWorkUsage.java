// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions;

import io.cratis.fundamentals.execution.CorrelationId;

/** Exercises the correlation-id API from Java without value-class name mangling. */
public final class JavaUnitOfWorkUsage {
    private JavaUnitOfWorkUsage() {
    }

    /** Begins and looks up a unit of work using equal correlation identifiers. */
    public static boolean beginAndFind(IUnitOfWorkManager manager) {
        CorrelationId id = CorrelationId.create();
        IUnitOfWork unitOfWork = manager.begin(id);
        CorrelationId actual = unitOfWork.getCorrelationId();
        return actual.equals(id)
            && !actual.equals(CorrelationId.NOT_SET)
            && manager.tryGetFor(new CorrelationId(actual.value())) == unitOfWork;
    }
}
