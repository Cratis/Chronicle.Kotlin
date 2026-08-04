// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventStoreSubscriptions

import kotlin.reflect.KClass

/**
 * Defines a builder for configuring an event store subscription.
 */
interface IEventStoreSubscriptionBuilder {
    /**
     * Specify an event type to subscribe to. If none are specified, all event types are subscribed to.
     *
     * @param eventClass The event type to include.
     */
    fun <TEvent : Any> withEventType(eventClass: KClass<TEvent>): IEventStoreSubscriptionBuilder
}
