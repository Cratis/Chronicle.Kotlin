// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle;

import io.cratis.chronicle.auditing.Causation;
import io.cratis.chronicle.identity.Identity;
import io.cratis.chronicle.java.BlockingEventSequence;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Ordinary Java usage of explicit operation metadata and transactions. */
public final class OperationContextJavaUsage {
    private OperationContextJavaUsage() {
    }

    public static OperationContext build(UUID correlationId) {
        return OperationContext.builder()
            .correlationId(correlationId)
            .causedBy(new Identity("java-user", "Java User", "", null))
            .causation(Causation.of(Instant.EPOCH, "JavaCall", Map.of("source", "test")))
            .build();
    }

    public static void transact(BlockingEventSequence sequence, OperationContext context, Object first, Object second) {
        try (var transaction = sequence.beginUnitOfWork(context)) {
            transaction.append("first", first);
            transaction.append("second", second);
            transaction.commit();
        }
    }
}
