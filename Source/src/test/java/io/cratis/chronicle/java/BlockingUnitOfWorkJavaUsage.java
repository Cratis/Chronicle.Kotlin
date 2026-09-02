// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.eventSequences.AppendError;
import io.cratis.chronicle.eventSequences.ConstraintViolation;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation;
import java.util.List;

/** Ordinary-Java inspection of a blocking unit of work without unwrap or value-class calls. */
public final class BlockingUnitOfWorkJavaUsage {
    private BlockingUnitOfWorkJavaUsage() {
    }

    public record Snapshot(
        List<ConstraintViolation> constraintViolations,
        List<ConcurrencyViolation> concurrencyViolations,
        List<AppendError> appendErrors,
        List<Object> stagedEvents,
        Long lastCommittedSequenceNumber) {
    }

    public static Snapshot inspect(BlockingUnitOfWork unitOfWork) {
        return new Snapshot(
            unitOfWork.getConstraintViolations(),
            unitOfWork.getConcurrencyViolations(),
            unitOfWork.getAppendErrors(),
            unitOfWork.getStagedEvents(),
            unitOfWork.getLastCommittedSequenceNumber());
    }
}
