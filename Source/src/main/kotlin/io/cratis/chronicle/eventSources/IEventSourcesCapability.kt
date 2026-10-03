// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * Optional capability of an [io.cratis.chronicle.IEventStore]: it exposes the event source definitions.
 *
 * It is a separate interface, rather than a member of `IEventStore`, so that event store implementations
 * written before event sources existed - in Kotlin or Java, compiled or not - keep compiling and linking
 * unchanged. [io.cratis.chronicle.EventStore] implements it; read the definitions through the
 * `IEventStore.eventSources` extension, which reports [IEventSources.unsupported] for a store without it.
 */
interface IEventSourcesCapability {
    /** The event source and stream definitions of this event store. */
    val eventSources: IEventSources
}
