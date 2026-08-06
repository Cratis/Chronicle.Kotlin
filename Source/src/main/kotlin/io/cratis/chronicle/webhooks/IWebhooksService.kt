// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks

interface IWebhooksService {
    /**
     * Discovers and registers webhooks from the given definers - instances of classes implementing
     * [IWebhookDefiner] and annotated with [Webhook].
     *
     * @param definers The candidate instances to discover webhooks from.
     */
    suspend fun register(vararg definers: Any)

    /**
     * Registers a webhook imperatively.
     *
     * @param id The identifier of the webhook to register.
     * @param targetUrl The target URL to send events to.
     * @param configure The callback for configuring the webhook.
     */
    suspend fun register(id: String, targetUrl: String, configure: (IWebhookDefinitionBuilder) -> Unit)

    /**
     * Get all registered webhook definitions for this event store.
     */
    suspend fun getAll(): List<ObservationWebhooks.WebhookDefinition>

    /**
     * Removes a webhook by its identifier.
     *
     * @param id The identifier of the webhook to remove.
     */
    suspend fun remove(id: String)
}
