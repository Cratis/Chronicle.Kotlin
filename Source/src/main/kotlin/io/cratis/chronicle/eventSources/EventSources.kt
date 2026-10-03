// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import Cratis.Chronicle.Contracts.EventSources.EventSourcesGrpcKt
import Cratis.Chronicle.Contracts.EventSources.Eventsources
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.artifacts.eventSources
import io.cratis.chronicle.eventSequences.ChronicleCommandRejected
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * The default [IEventSources]: discovers [EventSource] definitions from the client artifacts and
 * registers them through the kernel's event sources service.
 *
 * @param eventStoreName The name of the event store the definitions belong to.
 * @param stub The event sources service; `null` for an instance that only resolves definitions.
 * @param artifacts The client artifacts the definitions are discovered from.
 */
class EventSources(
    private val eventStoreName: String,
    private val stub: EventSourcesGrpcKt.EventSourcesCoroutineStub?,
    private val artifacts: IClientArtifacts
) : IEventSources {
    private val discovered: Discovered by lazy { discover() }

    override val all: List<EventSourceDefinition> get() = discovered.byType.values.toList()

    override fun getFor(type: KClass<*>): EventSourceDefinition =
        discovered.byType[type] ?: throw UnknownEventSource(type.qualifiedName ?: type.toString())

    override fun getFor(name: String): EventSourceDefinition =
        discovered.byName[name] ?: throw UnknownEventSource(name)

    override suspend fun register() {
        val definitions = all
        if (definitions.isEmpty()) return

        val service = checkNotNull(stub) { "Event sources can only be registered when they belong to an event store." }
        val request = Eventsources.RegisterEventSourcesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllSources(definitions.map { it.toContract() })
            .build()

        val result = service.registerEventSources(request)
        if (!result.isAuthorized) {
            throw ChronicleCommandRejected(
                "register event sources",
                result.authorizationFailureReason.ifEmpty { "not authorized" }
            )
        }
        if (result.exceptionMessagesCount > 0) {
            throw ChronicleCommandRejected("register event sources", result.exceptionMessagesList.joinToString("; "))
        }
    }

    private fun discover(): Discovered {
        val definitions = artifacts.eventSources.map { describe(it) }

        definitions.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.let { duplicate ->
            throw DuplicateEventSourceName(duplicate.key, duplicate.value.map { it.type })
        }

        return Discovered(
            byType = definitions.associateBy { it.type },
            byName = definitions.associateBy { it.name }
        )
    }

    private class Discovered(val byType: Map<KClass<*>, EventSourceDefinition>, val byName: Map<String, EventSourceDefinition>)

    companion object {
        /** The suffix removed from a class name to derive the default event source name. */
        private const val SUFFIX = "EventSource"

        /**
         * Describes the definition carried by [type].
         *
         * @throws InvalidEventSourceDefinition The type is not annotated, or a name is blank.
         * @throws DuplicateEventStreamName The type declares the same stream twice.
         */
        fun describe(type: KClass<*>): EventSourceDefinition {
            val annotation = type.findAnnotation<EventSource>()
                ?: throw InvalidEventSourceDefinition(type, "the class is not annotated with @EventSource")
            val simpleName = type.simpleName.orEmpty()
            val name = annotation.name.ifEmpty {
                if (simpleName.length > SUFFIX.length && simpleName.endsWith(SUFFIX)) simpleName.dropLast(SUFFIX.length) else simpleName
            }
            if (name.isBlank()) throw InvalidEventSourceDefinition(type, "the event source name is blank")

            val streams = annotation.streams.map {
                if (it.name.isBlank()) throw InvalidEventSourceDefinition(type, "a stream name is blank")
                EventStreamDefinition(it.name, it.description, it.concurrency.toSet())
            }
            streams.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.let {
                throw DuplicateEventStreamName(type, it.key)
            }

            return EventSourceDefinition(type, name, annotation.description, annotation.concurrency.toSet(), streams)
        }
    }
}

internal fun EventSourceDefinition.toContract(): Eventsources.EventSourceDefinition =
    Eventsources.EventSourceDefinition.newBuilder()
        .setName(name)
        .setDescription(description)
        .setOwner(Eventsources.EventSourceOwner.Client)
        .setConcurrencyValue(concurrency.toFlags())
        .addAllStreams(
            streams.map {
                Eventsources.EventStreamDefinition.newBuilder()
                    .setName(it.name)
                    .setDescription(it.description)
                    .setConcurrencyValue(it.concurrency.toFlags())
                    .build()
            }
        )
        .build()
