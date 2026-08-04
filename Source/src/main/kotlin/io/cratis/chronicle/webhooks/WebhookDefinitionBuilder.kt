// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class WebhookDefinitionBuilder : IWebhookDefinitionBuilder {
    private val eventTypes = mutableSetOf<EventTypeDescriptor>()
    private val headers = mutableMapOf<String, String>()
    private var eventSequenceId: EventSequenceId = EventSequenceId.eventLog
    private var authorization: Authorization? = null
    private var isReplayable = true
    private var isActive = true

    override fun onEventSequence(eventSequenceId: EventSequenceId): IWebhookDefinitionBuilder {
        this.eventSequenceId = eventSequenceId
        return this
    }

    override fun withBasicAuth(username: String, password: String): IWebhookDefinitionBuilder {
        authorization = Authorization.Basic(username, password)
        return this
    }

    override fun withBearerToken(token: String): IWebhookDefinitionBuilder {
        authorization = Authorization.Bearer(token)
        return this
    }

    override fun withOAuth(authority: String, clientId: String, clientSecret: String): IWebhookDefinitionBuilder {
        authorization = Authorization.OAuth(authority, clientId, clientSecret)
        return this
    }

    override fun withHeader(key: String, value: String): IWebhookDefinitionBuilder {
        headers[key] = value
        return this
    }

    override fun <TEvent : Any> withEventType(eventClass: KClass<TEvent>): IWebhookDefinitionBuilder {
        val ann = eventClass.findAnnotation<EventType>() ?: return this
        val id = ann.id.ifEmpty { eventClass.simpleName!! }
        eventTypes.add(EventTypeDescriptor(EventTypeId(id), EventTypeGeneration(ann.generation), ann.tombstone))
        return this
    }

    override fun notReplayable(): IWebhookDefinitionBuilder {
        isReplayable = false
        return this
    }

    override fun notActive(): IWebhookDefinitionBuilder {
        isActive = false
        return this
    }

    /**
     * Builds the [ObservationWebhooks.WebhookDefinition].
     *
     * @param id The identifier of the webhook.
     * @param targetUrl The target URL to send events to.
     */
    fun build(id: String, targetUrl: String): ObservationWebhooks.WebhookDefinition {
        val targetBuilder = ObservationWebhooks.WebhookTarget.newBuilder()
            .setUrl(targetUrl)
            .putAllHeaders(headers)

        authorization?.let { auth ->
            val oneOfBuilder = ObservationWebhooks.OneOf_BasicAuthorization_BearerTokenAuthorization_OAuthAuthorization.newBuilder()
            when (auth) {
                is Authorization.Basic -> oneOfBuilder.setValue0(
                    ObservationWebhooks.BasicAuthorization.newBuilder()
                        .setUsername(auth.username)
                        .setPassword(auth.password)
                        .build()
                )
                is Authorization.Bearer -> oneOfBuilder.setValue1(
                    ObservationWebhooks.BearerTokenAuthorization.newBuilder()
                        .setToken(auth.token)
                        .build()
                )
                is Authorization.OAuth -> oneOfBuilder.setValue2(
                    ObservationWebhooks.OAuthAuthorization.newBuilder()
                        .setAuthority(auth.authority)
                        .setClientId(auth.clientId)
                        .setClientSecret(auth.clientSecret)
                        .build()
                )
            }
            targetBuilder.setAuthorization(oneOfBuilder.build())
        }

        return ObservationWebhooks.WebhookDefinition.newBuilder()
            .setIdentifier(id)
            .setEventSequenceId(eventSequenceId.value)
            .addAllEventTypes(
                eventTypes.map { eventType ->
                    ObservationWebhooks.EventType.newBuilder()
                        .setId(eventType.id.value)
                        .setGeneration(eventType.generation.value)
                        .setTombstone(eventType.tombstone)
                        .build()
                }
            )
            .setTarget(targetBuilder.build())
            .setIsReplayable(isReplayable)
            .setIsActive(isActive)
            .build()
    }

    private sealed class Authorization {
        data class Basic(val username: String, val password: String) : Authorization()
        data class Bearer(val token: String) : Authorization()
        data class OAuth(val authority: String, val clientId: String, val clientSecret: String) : Authorization()
    }
}
