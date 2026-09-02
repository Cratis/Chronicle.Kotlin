// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;
import io.cratis.chronicle.events.EventTypeDescriptor;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.IReadModelsService;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

/**
 * The read-back bridges a Java caller reaches for, written the way a reader would write them.
 *
 * Each of these wraps something Java cannot call directly - a {@code suspend fun} or a
 * {@code Flow} - so the bridge is the whole of the Java story for that operation. Nothing here
 * runs; compiling it is the assertion.
 */
public final class JavaBridgeReadbackUsage {

    private JavaBridgeReadbackUsage() {
    }

    /** A read model to read back. */
    @ReadModel
    public record Product(String id, String name, double price) {
    }

    /** An observer, so there is something to ask the tail for. */
    @Reducer
    public static class Products {
        public Product renamed(Product product) {
            return product;
        }
    }

    /** The tail as far as one observer is concerned, rather than of the whole sequence. */
    public static long tailForObserver(IEventLog eventLog) {
        return EventLogJavaBridge.getTailSequenceNumberForObserver(eventLog, Products.class);
    }

    /** Replaying every instance, and replaying with the event count capped. */
    public static List<Product> replayed(IReadModelsService readModels) {
        List<Product> everything = ReadModelsJavaBridge.getInstances(readModels, Product.class);
        List<Product> capped = ReadModelsJavaBridge.getInstances(readModels, Product.class, 1_000L);
        return everything.size() > capped.size() ? everything : capped;
    }

    /**
     * Naming event types to narrow a concurrency scope.
     *
     * {@code EventTypeId} and {@code EventTypeGeneration} are value classes Java cannot construct,
     * and a String-taking constructor on {@link EventTypeDescriptor} would erase to the primary
     * one's JVM signature - so {@code parse} is the door, and it has to be static to be usable.
     */
    public static ConcurrencyScope scopedToPaymentEvents() {
        return new ConcurrencyScopeBuilder()
            .withEventSourceId()
            .withEventType(EventTypeDescriptor.parse("PaymentProcessed"))
            .withEventType(EventTypeDescriptor.parse("PaymentFailed+2"))
            .build();
    }

    /** Watching a stored page, and cancelling the subscription to release the change stream. */
    public static void observeMaterializedPage(IReadModelsService readModels) {
        var subscription = ReadModelsJavaBridge.observeMaterializedInstances(
            readModels,
            Product.class,
            0,
            50,
            products -> System.out.println("Products updated: " + products.size() + " in view"),
            error -> System.out.println("Stopped observing: " + error.getMessage()));

        subscription.cancel(null);
    }

    /** Watching one read model, with somewhere for a failed stream to go. */
    public static void watchWithErrorHandling(IReadModelsService readModels) {
        ReadModelsJavaBridge.watch(
            readModels,
            Product.class,
            changeset -> System.out.println("changed: " + changeset.getModelKey()),
            error -> System.out.println("stopped: " + error.getMessage()));
    }
}
