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

        val request = ObservationWebhooks.AddWebhooksRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllWebhooks(definitions)
            .build()

        stub.addWebhooks(request)
    }

    override suspend fun register(id: String, targetUrl: String, configure: (IWebhookDefinitionBuilder) -> Unit) {
        val builder = WebhookDefinitionBuilder()
        configure(builder)
        val definition = builder.build(id, targetUrl)

        val request = ObservationWebhooks.AddWebhooksRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addWebhooks(definition)
            .build()

        stub.addWebhooks(request)
    }

    override suspend fun getAll(): List<ObservationWebhooks.WebhookDefinition> {
        val request = ObservationWebhooks.GetWebhooksRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()

        return stub.getWebhooks(request).dataList.map { it.toDefinition() }
    }

    override suspend fun remove(id: String) {
        val request = ObservationWebhooks.RemoveWebhooksRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addWebhooks(id)
            .build()

        stub.removeWebhooks(request)
    }
}

/**
 * Converts the flattened wire read shape back into a [ObservationWebhooks.WebhookDefinition],
 * matching how the .NET client reconstructs it - the authorization details behind [WebhookTarget]
 * are never sent back on read, so the reconstructed target carries only its URL and headers.
 */
private fun ObservationWebhooks.WebhookDetailsResponse.toDefinition(): ObservationWebhooks.WebhookDefinition =
    ObservationWebhooks.WebhookDefinition.newBuilder()
        .setEventSequenceId(eventSequenceId)
        .setIdentifier(identifier)
        .addAllEventTypes(eventTypesList)
        .setTarget(
            ObservationWebhooks.WebhookTarget.newBuilder()
                .setUrl(url)
                .putAllHeaders(headersMap)
                .build()
        )
        .setIsReplayable(isReplayable)
        .setIsActive(isActive)
        .build()
