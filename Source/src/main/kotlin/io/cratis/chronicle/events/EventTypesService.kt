// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.Events.Events
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class EventTypesService(
    private val eventStoreName: String,
    private val stub: EventTypesGrpcKt.EventTypesCoroutineStub
) {
    suspend fun register(vararg eventClasses: KClass<*>) {
        val registrations = eventClasses.mapNotNull { cls ->
            val ann = cls.findAnnotation<EventType>() ?: return@mapNotNull null
            val id = ann.id.ifEmpty { cls.simpleName!! }
            Events.EventTypeRegistration.newBuilder()
                .setType(
                    Events.EventType.newBuilder()
                        .setId(id)
                        .setGeneration(ann.generation)
                        .setTombstone(ann.tombstone)
                        .build()
                )
                .setSchema("{}")
                .build()
        }
        if (registrations.isEmpty()) return
        val request = Events.RegisterEventTypesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllTypes(registrations)
            .setDisableValidation(false)
            .build()
        stub.register(request)
    }
}
