// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.Events.Events
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder
import io.cratis.chronicle.events.migrations.IEventTypeMigration
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

class EventTypesService(
    private val eventStoreName: String,
    private val stub: EventTypesGrpcKt.EventTypesCoroutineStub
) : IEventTypesService {
    private val registeredEventTypes: MutableSet<EventTypeDescriptor> = ConcurrentHashMap.newKeySet()

    /**
     * Register one or more event types with the event store. [eventClasses] may contain plain
     * `@EventType`-annotated classes and/or [IEventTypeMigration] classes describing how to
     * migrate between two generations of the same event type — both are discovered by reflection
     * and merged into a single registration per event type id.
     */
    override suspend fun register(vararg eventClasses: KClass<*>) {
        recordEventTypeDescriptors(eventClasses.toList())
        val registrations = buildRegistrations(eventClasses.toList())
        if (registrations.isEmpty()) return
        val request = Events.RegisterEventTypesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllTypes(registrations)
            .setDisableValidation(false)
            .build()
        stub.register(request)
    }

    /** Register a single event type with the event store. */
    override suspend fun registerSingle(eventClass: KClass<*>) {
        recordEventTypeDescriptors(listOf(eventClass))
        val registration = buildRegistrations(listOf(eventClass)).firstOrNull() ?: return
        val request = Events.RegisterSingleEventTypeRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setType(registration)
            .build()
        stub.registerSingle(request)
    }

    /** Get all known generations, and their migrations, for the given [eventTypeId]. */
    override suspend fun getAllGenerationsForEventType(eventTypeId: String): List<Events.EventTypeRegistration> {
        val request = Events.GetEventTypeGenerationsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setEventTypeId(eventTypeId)
            .build()
        return stub.getAllGenerationsForEventType(request).itemsList
    }

    override fun getRegisteredEventTypes(): List<EventTypeDescriptor> = registeredEventTypes.toList()

    /** Records the [EventTypeDescriptor] for every `@EventType`-annotated class among [classes]. */
    private fun recordEventTypeDescriptors(classes: List<KClass<*>>) {
        classes.forEach { cls ->
            val ann = cls.findAnnotation<EventType>() ?: return@forEach
            val id = ann.id.ifEmpty { cls.simpleName!! }
            registeredEventTypes.add(EventTypeDescriptor(EventTypeId(id), EventTypeGeneration(ann.generation), ann.tombstone))
        }
    }

    private fun buildRegistrations(classes: List<KClass<*>>): List<Events.EventTypeRegistration> {
        val eventEntries = classes.mapNotNull { cls -> cls.findAnnotation<EventType>()?.let { it to cls } }
        val eventsById = eventEntries.groupBy { (ann, cls) -> ann.id.ifEmpty { cls.simpleName!! } }

        val migrations = classes
            .filter { it.isSubclassOf(IEventTypeMigration::class) }
            .map { it.instantiateMigration() }
        val migrationsById = migrations.groupBy { it.targetEventTypeId() }

        val ids = (eventsById.keys + migrationsById.keys).distinct()
        return ids.map { id -> buildRegistration(id, eventsById[id].orEmpty(), migrationsById[id].orEmpty()) }
    }

    private fun buildRegistration(
        id: String,
        eventEntries: List<Pair<EventType, KClass<*>>>,
        migrations: List<IEventTypeMigration<Any, Any>>
    ): Events.EventTypeRegistration {
        val latest = eventEntries.maxByOrNull { (ann, _) -> ann.generation }
        val generation = latest?.first?.generation ?: migrations.maxOf { it.targetGeneration() }
        val tombstone = latest?.first?.tombstone ?: false
        val latestClass = latest?.second ?: migrations.maxByOrNull { it.targetGeneration() }?.targetClass
        val schema = latestClass?.let { JsonSchemaGenerator.generate(it) } ?: "{}"

        val builder = Events.EventTypeRegistration.newBuilder()
            .setSchema(schema)
            .setType(
                Events.EventType.newBuilder()
                    .setId(id)
                    .setGeneration(generation)
                    .setTombstone(tombstone)
                    .build()
            )

        val addedGenerations = mutableSetOf<Int>()
        eventEntries.forEach { (ann, cls) -> builder.addGenerationIfAbsent(addedGenerations, ann.generation, cls) }

        migrations.forEach { migration ->
            val fromGeneration = migration.sourceGeneration()
            val toGeneration = migration.targetGeneration()
            check(toGeneration == fromGeneration + 1) {
                "Migration from generation $fromGeneration to $toGeneration for event type '$id' is invalid. " +
                    "The target generation must be exactly one more than the source generation."
            }

            val upcastBuilder = EventTypeMigrationBuilder<Any, Any>()
            migration.upcast(upcastBuilder)
            val downcastBuilder = EventTypeMigrationBuilder<Any, Any>()
            migration.downcast(downcastBuilder)

            builder.addMigrations(
                Events.EventTypeMigrationDefinition.newBuilder()
                    .setFromGeneration(fromGeneration)
                    .setToGeneration(toGeneration)
                    .setUpcastJmesPath(upcastBuilder.toJson())
                    .setDowncastJmesPath(downcastBuilder.toJson())
                    .build()
            )

            builder.addGenerationIfAbsent(addedGenerations, fromGeneration, migration.sourceClass)
            builder.addGenerationIfAbsent(addedGenerations, toGeneration, migration.targetClass)
        }

        return builder.build()
    }

    private fun Events.EventTypeRegistration.Builder.addGenerationIfAbsent(added: MutableSet<Int>, generation: Int, cls: KClass<*>?) {
        if (added.add(generation)) {
            addGenerations(
                Events.EventTypeGenerationDefinition.newBuilder()
                    .setGeneration(generation)
                    .setSchema(cls?.let { JsonSchemaGenerator.generate(it) } ?: "{}")
                    .build()
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun KClass<*>.instantiateMigration(): IEventTypeMigration<Any, Any> =
    (objectInstance ?: createInstance()) as IEventTypeMigration<Any, Any>

private fun IEventTypeMigration<Any, Any>.targetEventTypeId(): String {
    val ann = targetClass.findAnnotation<EventType>()
        ?: error("Migration target class '${targetClass.simpleName}' must be annotated with @EventType.")
    return ann.id.ifEmpty { targetClass.simpleName!! }
}

private fun IEventTypeMigration<Any, Any>.targetGeneration(): Int =
    (
        targetClass.findAnnotation<EventType>()
            ?: error("Migration target class '${targetClass.simpleName}' must be annotated with @EventType.")
        ).generation

private fun IEventTypeMigration<Any, Any>.sourceGeneration(): Int =
    (
        sourceClass.findAnnotation<EventType>()
            ?: error("Migration source class '${sourceClass.simpleName}' must be annotated with @EventType.")
        ).generation
