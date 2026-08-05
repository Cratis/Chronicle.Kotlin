// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks;

import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.webhooks.IWebhookDefiner;
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder;
import io.cratis.chronicle.webhooks.Webhook;

import io.cratis.chronicle.java.WebhookDefinitionBuilderJavaBridge;
import io.cratis.chronicle.java.WebhooksServiceJavaBridge;

import java.util.List;

/**
 * Notifies an external HR system whenever an employee is hired.
 *
 * Discovered by {@code IWebhooksService.register} because it implements {@link IWebhookDefiner}
 * and is annotated with {@link Webhook} — no imperative registration call needed.
 */
@Webhook(targetUrl = "https://hooks.example.com/hr/employee-hired")
class EmployeeHiredWebhook implements IWebhookDefiner {
    @Override
    public void define(IWebhookDefinitionBuilder builder) {
        WebhookDefinitionBuilderJavaBridge.withEventType(builder, EmployeeHired.class)
            .withBearerToken("demo-webhook-token");
    }
}

public class Webhooks {
    /** Lists the webhooks currently registered for this event store. */
    public static void listWebhooks(EventStore store) {
        List<ObservationWebhooks.WebhookDefinition> webhooks = WebhooksServiceJavaBridge.getAll(store.getWebhooks());
        if (webhooks.isEmpty()) {
            System.out.println("[webhooks] No webhooks registered.");
            return;
        }
        System.out.println("[webhooks] " + webhooks.size() + " webhook(s):");
        for (ObservationWebhooks.WebhookDefinition webhook : webhooks) {
            System.out.println("  " + webhook.getIdentifier() + " -> " + webhook.getTarget().getUrl() +
                " (active=" + webhook.getIsActive() + ", replayable=" + webhook.getIsReplayable() + ")");
        }
    }
}
