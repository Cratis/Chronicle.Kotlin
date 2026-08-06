// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.webhooks.IWebhookDefiner
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder
import io.cratis.chronicle.webhooks.Webhook

/**
 * Notifies an external HR system whenever an employee is hired.
 *
 * Discovered by [io.cratis.chronicle.webhooks.IWebhooksService.register] because it implements
 * [IWebhookDefiner] and is annotated with [Webhook] — no imperative registration call needed.
 */
@Webhook(targetUrl = "https://hooks.example.com/hr/employee-hired")
class EmployeeHiredWebhook : IWebhookDefiner {
    override fun define(builder: IWebhookDefinitionBuilder) {
        builder
            .withEventType(EmployeeHired::class)
            .withBearerToken("demo-webhook-token")
    }
}

/** Lists the webhooks currently registered for this event store. */
suspend fun listWebhooks(store: EventStore) {
    val webhooks = store.webhooks.getAll()
    if (webhooks.isEmpty()) {
        println("[webhooks] No webhooks registered.")
        return
    }
    println("[webhooks] ${webhooks.size} webhook(s):")
    webhooks.forEach { webhook ->
        println("  ${webhook.identifier} -> ${webhook.target.url} (active=${webhook.isActive}, replayable=${webhook.isReplayable})")
    }
}
