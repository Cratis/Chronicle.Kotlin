// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
import Cratis.Chronicle.Contracts.Observation.Webhooks.WebhooksGrpcKt
import kotlin.reflect.full.findAnnotation

class WebhooksService(
    private val eventStoreName: String,
    private val stub: WebhooksGrpcKt.WebhooksCoroutineStub
) : IWebhooksService {

    override suspend fun register(vararg definers: Any) {
        val definitions = definers.mapNotNull { definer ->
            if (definer !is IWebhookDefiner) return@mapNotNull null
            val ann = definer::class.findAnnotation<Webhook>() ?: return@mapNotNull null
            val id = ann.id.ifEmpty { definer::class.simpleName!! }

            val builder = WebhookDefinitionBuilder()
            definer.define(builder)
            builder.build(id, ann.targetUrl)
        }

        if (definitions.isEmpty()) return

        val request = ObservationWebhooks.AddWebhooks.newBuilder()
            .setEventStore(eventStoreName)
            .setOwner(ObservationWebhooks.ObserverOwner.Client)
            .addAllWebhooks(definitions)
            .build()

        stub.add(request)
    }

    override suspend fun register(id: String, targetUrl: String, configure: (IWebhookDefinitionBuilder) -> Unit) {
        val builder = WebhookDefinitionBuilder()
        configure(builder)
        val definition = builder.build(id, targetUrl)

        val request = ObservationWebhooks.AddWebhooks.newBuilder()
            .setEventStore(eventStoreName)
            .setOwner(ObservationWebhooks.ObserverOwner.Client)
            .addWebhooks(definition)
            .build()

        stub.add(request)
    }

    override suspend fun getAll(): List<ObservationWebhooks.WebhookDefinition> {
        val request = ObservationWebhooks.GetWebhooksRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()

        return stub.getWebhooks(request).itemsList
    }

    override suspend fun remove(id: String) {
        val request = ObservationWebhooks.RemoveWebhooks.newBuilder()
            .setEventStore(eventStoreName)
            .addWebhooks(id)
            .build()

        stub.remove(request)
    }
}
