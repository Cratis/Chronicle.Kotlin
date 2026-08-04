// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

/**
 * Defines a discoverable class capable of defining a webhook.
 */
interface IWebhookDefiner {
    /**
     * Defines the webhook using the given builder.
     *
     * @param builder The [IWebhookDefinitionBuilder] to define the webhook with.
     */
    fun define(builder: IWebhookDefinitionBuilder)
}
