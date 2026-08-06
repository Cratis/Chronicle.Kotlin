// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations;

import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.eventSequences.IEventSequence;
import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventSequenceJavaBridge;
import io.cratis.chronicle.java.EventSequenceOperationsJavaBridge;
import io.cratis.chronicle.java.EventSourceOperationsJavaBridge;
import java.util.List;

/**
 * Java use of the composed event sequence operations, exercised from Kotlin specs.
 *
 * Java has no suspend functions, no lambdas with receiver, and no default arguments, so every one of
 * those is reached through a bridge. These usages fail to compile if a bridge is removed, renamed,
 * or grows a parameter Java can no longer omit.
 */
public final class JavaEventSequenceOperationsUsage {

    private JavaEventSequenceOperationsUsage() {
    }

    /** Composes two event sources with different shaping and performs the batch. */
    public static List<AppendResult> composeAndPerform(IEventSequence sequence, Object first, Object second) {
        IEventSequenceOperations operations = EventSequenceOperationsJavaBridge.operationsFor(sequence);

        EventSequenceOperationsJavaBridge.forEventSourceId(operations, "customer-1", source -> {
            EventSourceOperationsJavaBridge.withConcurrencyScope(
                source,
                scope -> ConcurrencyScopeBuilderJavaBridge.withSequenceNumber(scope, 3).withEventSourceId());
            EventSourceOperationsJavaBridge.append(source, first);
        });

        EventSequenceOperationsJavaBridge.forEventSourceId(operations, "customer-2", source ->
            EventSourceOperationsJavaBridge.append(source, second, "Onboarding"));

        return EventSequenceOperationsJavaBridge.perform(operations);
    }

    /** Inspects what a composed operation is about to send, without sending it. */
    public static List<EventForEventSourceId> stagedEvents(IEventSequence sequence, Object event) {
        IEventSequenceOperations operations = EventSequenceOperationsJavaBridge.operationsFor(sequence);
        EventSequenceOperationsJavaBridge.forEventSourceId(operations, "customer-1", source ->
            EventSourceOperationsJavaBridge.append(source, event));
        return operations.getEventsToAppend();
    }

    /** Appends across event sources directly, without composing operations first. */
    public static List<AppendResult> appendManyAcrossEventSources(
        IEventSequence sequence,
        Object first,
        Object second) {
        return EventSequenceJavaBridge.appendMany(
            sequence,
            List.of(new EventForEventSourceId("customer-1", first), new EventForEventSourceId("customer-2", second)));
    }
}
