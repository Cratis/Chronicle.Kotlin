// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.events.EventType;

/**
 * The documented Java getting-started flow, exactly as a reader would write it.
 *
 * This is the fixture behind the claim that Java gets the same three lines Kotlin does. It used to
 * take a {@code BuildersKt.runBlocking} with a hand-cast {@code Continuation} to append one event;
 * if that ever becomes true again, this stops compiling.
 *
 * Nothing here runs - compiling it is the assertion.
 */
public final class JavaClientFlowUsage {

    private JavaClientFlowUsage() {
    }

    /** The event being appended. */
    @EventType
    public record TestEvent(String message) {
    }

    /** Connect, get the event store, append. No continuation, no registration call. */
    public static void clientFlow() {
        var client = BlockingChronicleClient.connect(ChronicleOptions.development());
        var eventStore = client.getEventStore("ChronicleConsole");

        eventStore.getEventLog().append("some-event-source", new TestEvent("Hello world!"));
    }

    /** The same, closed properly, since the client is {@link AutoCloseable}. */
    public static void clientFlowClosingTheClient() {
        try (var client = BlockingChronicleClient.connect(ChronicleOptions.development())) {
            client.getEventStore("ChronicleConsole")
                .getEventLog()
                .append("some-event-source", new TestEvent("Hello world!"));
        }
    }

    /** Naming a namespace, and reading back, still without a coroutine in sight. */
    public static long readingBack() {
        var client = BlockingChronicleClient.connect(ChronicleOptions.development());
        var eventLog = client.getEventStore("ChronicleConsole", "Default").getEventLog();

        eventLog.append("some-event-source", new TestEvent("Hello world!"));
        eventLog.appendMany("some-event-source", java.util.List.of(
            new TestEvent("one"),
            new TestEvent("two")));

        if (!eventLog.hasEventsFor("some-event-source")) {
            throw new IllegalStateException("expected the events to be there");
        }

        eventLog.getForEventSourceIdAndEventTypes("some-event-source", TestEvent.class);
        return eventLog.getTailSequenceNumber("some-event-source");
    }
}
