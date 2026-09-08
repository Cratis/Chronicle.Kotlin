// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.eventSequences.IEventSequence;
import java.util.List;

/** Ordinary-Java calls to explicit-context bulk redaction APIs. */
public final class JavaRedactionUsage {
    private JavaRedactionUsage() {
    }

    public static void throughBlockingSequence(
        BlockingEventSequence sequence,
        OperationContext context,
        Class<?> eventType) {
        sequence.redactForEventSource("source-1", "erasure", context, eventType);
    }

    public static void throughBridge(
        IEventSequence sequence,
        OperationContext context,
        Class<?> eventType) {
        EventSequenceJavaBridge.redactForEventSource(
            sequence,
            "source-1",
            "erasure",
            context,
            List.of(eventType));
    }
}
