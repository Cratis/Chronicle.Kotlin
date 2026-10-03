// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("EventStoreEventSources")

package io.cratis.chronicle

import io.cratis.chronicle.eventSources.IEventSources
import io.cratis.chronicle.eventSources.IEventSourcesCapability

/**
 * The event source definitions of this event store, or [IEventSources.unsupported] when the implementation
 * predates event sources and does not implement [IEventSourcesCapability].
 */
val IEventStore.eventSources: IEventSources
    get() = (this as? IEventSourcesCapability)?.eventSources ?: IEventSources.unsupported
