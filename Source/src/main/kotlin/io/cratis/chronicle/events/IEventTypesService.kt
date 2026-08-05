// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.Events.Events
import kotlin.reflect.KClass

interface IEventTypesService {
    /**
     * Register one or more event types with the event store. [eventClasses] may contain plain
     * `@EventType`-annotated classes and/or [io.cratis.chronicle.events.migrations.IEventTypeMigration] classes
     * describing how to migrate between two generations of the same event type — both are discovered by
     * reflection and merged into a single registration per event type id.
     */
    suspend fun register(vararg eventClasses: KClass<*>)

    /** Register a single event type with the event store. */
    suspend fun registerSingle(eventClass: KClass<*>)

    /** Get all known generations, and their migrations, for the given [eventTypeId]. */
    suspend fun getAllGenerationsForEventType(eventTypeId: String): List<Events.EventTypeRegistration>
}
