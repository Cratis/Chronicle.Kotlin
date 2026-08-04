// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import io.cratis.chronicle.eventSequences.EventSequenceId
import kotlin.reflect.KClass

/**
 * Defines a fluent builder for configuring a webhook definition.
 */
interface IWebhookDefinitionBuilder {
    /**
     * Sets the event sequence that the webhook observer should be registered on.
     *
     * @param eventSequenceId The [EventSequenceId].
     */
    fun onEventSequence(eventSequenceId: EventSequenceId): IWebhookDefinitionBuilder

    /**
     * Use basic authentication.
     *
     * @param username The username.
     * @param password The password.
     */
    fun withBasicAuth(username: String, password: String): IWebhookDefinitionBuilder

    /**
     * Use bearer token authentication.
     *
     * @param token The bearer token.
     */
    fun withBearerToken(token: String): IWebhookDefinitionBuilder

    /**
     * Use OAuth authentication.
     *
     * @param authority The OAuth authority.
     * @param clientId The OAuth client id.
     * @param clientSecret The OAuth client secret.
     */
    fun withOAuth(authority: String, clientId: String, clientSecret: String): IWebhookDefinitionBuilder

    /**
     * Adds a header to the webhook requests.
     *
     * @param key The header key.
     * @param value The header value.
     */
    fun withHeader(key: String, value: String): IWebhookDefinitionBuilder

    /**
     * Adds an event type to the webhook observer. If none are specified, all event types are subscribed to.
     *
     * @param eventClass The event type to include.
     */
    fun <TEvent : Any> withEventType(eventClass: KClass<TEvent>): IWebhookDefinitionBuilder

    /**
     * Specifies that the webhook is not replayable.
     */
    fun notReplayable(): IWebhookDefinitionBuilder

    /**
     * Specifies that the webhook is not active.
     */
    fun notActive(): IWebhookDefinitionBuilder
}
