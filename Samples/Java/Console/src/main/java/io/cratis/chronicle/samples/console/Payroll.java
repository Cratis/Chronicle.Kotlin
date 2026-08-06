// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;

import java.util.List;

public class Payroll {
    /** The name of the (hypothetical) external event store the payroll system owns. */
    private static final String PAYROLL_EVENT_STORE_NAME = "PayrollEventStore";

    /** The identifier of this store's subscription to the payroll system's outbox. */
    private static final String PAYROLL_SUBSCRIPTION_ID = "payroll-inbox";

    /**
     * Subscribes this event store to the payroll event store's outbox for {@link PayrollRunCompleted}
     * events, and registers the {@link PayrollRunSummary} projection that consumes them.
     */
    public static void setupPayrollIntegration(EventStore store) {
        EventStoreSubscriptionsServiceJavaBridge.subscribe(
            store.getEventStoreSubscriptions(),
            PAYROLL_SUBSCRIPTION_ID,
            PAYROLL_EVENT_STORE_NAME,
            builder -> {
                EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, PayrollRunCompleted.class);
                return null; // Java lambda returning Unit
            }
        );
        ProjectionsServiceJavaBridge.register(store.getProjections(), PayrollRunSummary.class);
        System.out.println("[subscriptions] Subscribed '" + PAYROLL_SUBSCRIPTION_ID + "' to '" +
            PAYROLL_EVENT_STORE_NAME + "' for PayrollRunCompleted events.");
    }

    /** Lists the event store subscriptions currently registered for this event store. */
    public static void listEventStoreSubscriptions(EventStore store) {
        List<ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition> subscriptions =
            EventStoreSubscriptionsServiceJavaBridge.getAll(store.getEventStoreSubscriptions());
        if (subscriptions.isEmpty()) {
            System.out.println("[subscriptions] No event store subscriptions registered.");
            return;
        }
        System.out.println("[subscriptions] " + subscriptions.size() + " subscription(s):");
        for (ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition subscription : subscriptions) {
            System.out.println("  " + subscription.getIdentifier() + " <- " + subscription.getSourceEventStore() +
                " (" + subscription.getEventTypesList().size() + " event type filter(s))");
        }
    }
}
