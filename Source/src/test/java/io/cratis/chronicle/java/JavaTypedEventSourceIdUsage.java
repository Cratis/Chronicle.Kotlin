// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.concepts.EventSourceId;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.IEventSequence;
import java.util.List;

/** Compiled Java calls passing a typed event source id, asserted by the Kotlin specifications. */
public final class JavaTypedEventSourceIdUsage {
    private JavaTypedEventSourceIdUsage() {
    }

    /** A typed event source id declared in Java. */
    public record JavaCustomerId(String value) implements EventSourceId {
    }

    public static void callEveryOverload(IEventSequence sequence, Object event, AppendOptions options) {
        var blocking = new BlockingEventSequence(sequence);
        var id = new JavaCustomerId("customer-1");
        blocking.append(id, event);
        blocking.append(id, event, options);
        blocking.appendMany(id, List.of(event));
        blocking.appendMany(id, List.of(event), options);
        blocking.hasEventsFor(id);
        blocking.getTailSequenceNumber(id);
        blocking.getForEventSourceIdAndEventTypes(id, String.class);
        blocking.redactForEventSource(id, "reason", String.class);
    }
}
