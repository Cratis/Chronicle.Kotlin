// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences;

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.java.EventSequenceJavaBridge;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Compiled Java calls whose outgoing requests are asserted by the Kotlin wire specifications. */
public final class JavaAppendRoutingUsage {
    private JavaAppendRoutingUsage() {
    }

    public static AppendResult append(IEventSequence sequence, Object event, AppendOptions options) {
        var blocking = new BlockingEventSequence(sequence);
        return options == null
            ? blocking.append("source-1", event)
            : blocking.append("source-1", event, build(options));
    }

    public static List<AppendResult> appendMany(IEventSequence sequence, List<Object> events, AppendOptions options) {
        var blocking = new BlockingEventSequence(sequence);
        return options == null
            ? blocking.appendMany("source-1", events)
            : blocking.appendMany("source-1", events, build(options));
    }

    public static List<AppendResult> appendMixed(
            IEventSequence sequence,
            List<EventForEventSourceId> events,
            Map<String, ConcurrencyScope> scopes,
            UUID correlationId) {
        var copied = events.stream().map(event -> new EventForEventSourceId(
            event.getEventSourceId(), event.getEvent(), event.getEventStreamType(), event.getEventStreamId(),
            event.getEventSourceType(), event.getTags(), event.getOccurred(), event.getSubject(), event.getCausation()
        )).toList();
        return EventSequenceJavaBridge.appendMany(sequence, copied, scopes, correlationId);
    }

    private static AppendOptions build(AppendOptions options) {
        var builder = new AppendOptionsBuilder().tags(options.getTags()).causation(options.getCausation());
        if (options.getEventSourceType() != null) builder.eventSourceType(options.getEventSourceType());
        if (options.getEventStreamType() != null) builder.eventStreamType(options.getEventStreamType());
        if (options.getEventStreamId() != null) builder.eventStreamId(options.getEventStreamId());
        if (options.getSubject() != null) builder.subject(options.getSubject());
        if (options.getOccurred() != null) builder.occurred(options.getOccurred());
        if (options.getCorrelationId() != null) builder.correlationId(options.getCorrelationId());
        if (options.getConcurrencyScope() != null) builder.concurrencyScope(options.getConcurrencyScope());
        return builder.build();
    }
}
