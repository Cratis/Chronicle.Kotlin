// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.ReadModelExplorer.ReadModelExplorerGrpcKt
import Cratis.Chronicle.Contracts.ReadModelExplorer.Readmodelexplorer
import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import io.cratis.chronicle.Subject
import io.cratis.chronicle.compliance.ComplianceService
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.observation.ReducerRegistration
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/** Resolves the read model identifier for [this] class: the [ReadModel.id] override, or the class simple name. */
internal fun KClass<*>.readModelIdentifier(): String {
    val ann = findAnnotation<ReadModel>()
    return ann?.id?.ifEmpty { simpleName!! } ?: simpleName!!
}

class ReadModelsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: ReadModelsGrpcKt.ReadModelsCoroutineStub,
    materializedStub: MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub,
    private val readModelExplorerStub: ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub,
    complianceStub: ComplianceGrpcKt.ComplianceCoroutineStub,
    private val defaultSinkTypeId: String = WellKnownSinkTypes.MONGODB
) : IReadModelsService {
    private val compliance = ComplianceService(eventStoreName, namespace, complianceStub)
    internal lateinit var resolveEventSequence: (EventSequenceId) -> IEventSequence
    private val passiveReducers = ConcurrentHashMap<KClass<*>, Pair<Any, ReducerRegistration>>()

    internal fun registerReducer(reducer: Any, registration: ReducerRegistration) {
        registration.readModelClass?.let { readModelClass ->
            if (registration.isActive) passiveReducers.remove(readModelClass)
            else passiveReducers[readModelClass] = reducer to registration
        }
    }

    override val materialized: IMaterializedReadModels = MaterializedReadModels(eventStoreName, namespace, materializedStub)

    override suspend fun register(vararg readModelClasses: KClass<*>) {
        for (cls in readModelClasses) {
            registerWithObserver(cls, 0, "")
        }
    }

    /**
     * Registers a read model and associates it with the given observer type and identifier.
     * Called internally by [io.cratis.chronicle.observation.ReducersService] and
     * [io.cratis.chronicle.projections.ProjectionsService] so that observer info is derived
     * from the reducer/projection rather than from the [ReadModel] annotation.
     *
     * @param cls Read model class to register.
     * @param observerType 0 = NotSet, 1 = Reducer, 2 = Projection.
     * @param observerIdentifier Simple name of the reducer or projection that produces this model.
     * @param passive Whether the observer that produces this model is passive. A passive reducer has no
     *   observer writing it to a sink, so it registers with no sink and is reduced locally on read.
     *   A model annotated [Passive] is always registered that way.
     */
    internal suspend fun registerWithObserver(
        cls: KClass<*>,
        observerType: Int,
        observerIdentifier: String,
        passive: Boolean = false
    ) {
        val ann = cls.findAnnotation<ReadModel>()
        val identifier = cls.readModelIdentifier()
        val displayName = ann?.displayName?.ifEmpty { cls.simpleName!! } ?: cls.simpleName!!

        val definition = Readmodels.ReadModelDefinition.newBuilder()
            .setType(
                Readmodels.ReadModelType.newBuilder()
                    .setIdentifier(identifier)
                    .build()
            )
            .setContainerName(identifier)
            .setDisplayName(displayName)
            .setSink(
                Readmodels.SinkDefinition.newBuilder()
                    // lo=1 ensures the field serializes on the wire so C# protobuf-net
                    // sees a non-null Guid (Guid.Empty maps to 00000001-... on server side,
                    // which is used only as a configuration reference key).
                    .setConfigurationId(
                        Bcl.Guid.newBuilder().setLo(1L).setHi(0L).build()
                    )
                    .setTypeId(if (passive || cls.findAnnotation<Passive>() != null) WellKnownSinkTypes.NONE else defaultSinkTypeId)
                    .build()
            )
            .setSchema(JsonSchemaGenerator.generate(cls))
            .setObserverTypeValue(observerType)
            .setObserverIdentifier(observerIdentifier)
            .build()

        val request = Readmodels.RegisterManyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addReadModels(definition)
            .setSourceValue(1)
            .build()

        stub.registerMany(request)
    }

    override suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T? {
        passiveReducers[readModelClass]?.let { (reducer, registration) ->
            val events = sequenceFor(registration).getForEventSourceIdAndEventTypes(key, registration.eventClasses())
                .filter { registration.accepts(it) }
            if (events.isEmpty()) return null
            return fold(events, reducer, registration)?.let { release(readModelClass.java.cast(it)) }
        }

        val request = Readmodels.GetInstanceByKeyRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .setSessionId("")
            .build()

        val response = stub.getInstanceByKey(request)
        return if (response.readModel.isNullOrBlank() || response.readModel == "null") {
            null
        } else {
            chronicleGson.fromJson(response.readModel, readModelClass.java)
        }
    }

    override suspend fun <T : Any> getInstances(readModelClass: KClass<T>, eventCount: Long?): List<T> {
        passiveReducers[readModelClass]?.let { (reducer, registration) ->
            val events = sequenceFor(registration).getFromSequenceNumber(EventSequenceNumber.first, eventTypes = registration.eventClasses())
                .filter { registration.accepts(it) }
                .sortedBy { it.context.sequenceNumber }
            val bounded = if (eventCount == null) events else events.take(eventCount.coerceIn(0L, events.size.toLong()).toInt())
            return releaseMany(bounded.groupBy { it.context.eventSourceId }.values.mapNotNull { sourceEvents ->
                fold(sourceEvents, reducer, registration)?.let { readModelClass.java.cast(it) }
            })
        }

        val builder = Readmodels.GetAllInstancesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
        if (eventCount != null) builder.setEventCount(eventCount)

        return stub.getAllInstances(builder.build()).instancesList.map { chronicleGson.fromJson(it, readModelClass.java) }
    }

    private fun sequenceFor(registration: ReducerRegistration): IEventSequence =
        resolveEventSequence(EventSequenceId(registration.eventSequenceId))

    private fun ReducerRegistration.accepts(event: AppendedEvent): Boolean =
        (filters.eventSourceType.isEmpty() || event.context.eventSourceType == filters.eventSourceType) &&
            (filters.eventStreamType == "All" || event.context.eventStreamType == filters.eventStreamType) &&
            event.context.tags.containsAll(filters.filterTags)

    private fun ReducerRegistration.eventClasses(): List<KClass<*>> = handlers.values.map { it.eventClass }.distinct()

    private suspend fun fold(events: List<AppendedEvent>, reducer: Any, registration: ReducerRegistration): Any? {
        var state: Any? = null
        for (appendedEvent in events.sortedBy { it.context.sequenceNumber }) {
            val handler = registration.handlers[appendedEvent.context.eventType.id.value] ?: continue
            val event = chronicleGson.fromJson(appendedEvent.content, handler.eventClass.java)
            state = handler.invokeReducer(reducer, event, state, appendedEvent.context)
        }
        return state
    }

    override suspend fun <T : Any> getSnapshotsById(readModelClass: KClass<T>, key: String): List<ReadModelSnapshot<T>> {
        val request = Readmodelexplorer.AllSnapshotsForReadModelRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModel(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .setGrouping("")
            .build()

        return readModelExplorerStub.allSnapshotsForReadModel(request).dataList.map { it.toTyped(readModelClass) }
    }

    override fun <T : Any> watch(readModelClass: KClass<T>): Flow<ReadModelChangeset<T>> {
        val request = Readmodels.WatchRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .build()

        return stub.watch(request).map { it.toTyped(readModelClass) }
    }

    override suspend fun dehydrateSession(readModelClass: KClass<*>, key: String, sessionId: String) {
        val request = Readmodels.DehydrateSessionRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setReadModelIdentifier(readModelClass.readModelIdentifier())
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setReadModelKey(key)
            .setSessionId(sessionId)
            .build()

        stub.dehydrateSession(request)
    }

    override suspend fun <T : Any> release(instance: T, subject: String?): T {
        val resolvedSubject = subject ?: resolveSubject(instance) ?: return instance
        val schema = JsonSchemaGenerator.generate(instance::class)
        val payload = chronicleGson.toJson(instance)
        val released = compliance.release(resolvedSubject, schema, payload)
        @Suppress("UNCHECKED_CAST")
        return chronicleGson.fromJson(released, instance::class.java) as T
    }

    override suspend fun <T : Any> releaseMany(instances: List<T>): List<T> = instances.map { release(it) }

    /**
     * Resolves the compliance subject for [instance].
     *
     * Resolution order: a property annotated [io.cratis.chronicle.Subject] that carries a value,
     * falling back to a property named `id` (case-insensitive) - the convention every read model
     * followed before [io.cratis.chronicle.Subject] existed, kept so nothing that already relies on
     * it breaks.
     */
    private fun resolveSubject(instance: Any): String? {
        val properties = instance::class.memberProperties
        val explicit = properties
            .firstOrNull { it.hasAnnotation<Subject>() }
            ?.let { subjectValueOf(readProperty(it, instance)) }

        return explicit ?: properties
            .firstOrNull { it.name.equals("id", ignoreCase = true) }
            ?.let { subjectValueOf(readProperty(it, instance)) }
    }

    /**
     * Reads [property] off [instance], forcing accessibility first.
     *
     * A Kotlin data class exposes a public getter, so this changes nothing for one. A Java `record`
     * component has no such getter by Kotlin's reckoning - reflection falls back to the backing
     * field directly, which a record always declares `private final` - so without this the call
     * throws [IllegalAccessException] for every Java-authored read model.
     */
    private fun readProperty(property: KProperty1<out Any, *>, instance: Any): Any? {
        property.isAccessible = true
        return property.call(instance)
    }

    /** Unwraps a resolved subject property's value to the string a release call needs. */
    private fun subjectValueOf(value: Any?): String? = when (value) {
        null -> null
        is ConceptAs<*> -> subjectValueOf(value.value)
        is String -> value.ifEmpty { null }
        else -> value.toString().ifEmpty { null }
    }

    private fun <T : Any> Readmodelexplorer.ReadModelSnapshotResponse.toTyped(readModelClass: KClass<T>): ReadModelSnapshot<T> =
        ReadModelSnapshot(
            instance = chronicleGson.fromJson(instance, readModelClass.java),
            events = eventsList,
            occurred = if (hasOccurred()) occurred.value.toInstantOrNull() else null,
            correlationId = if (hasCorrelationId()) correlationId.toUUID() else null
        )

    private fun <T : Any> Readmodels.ReadModelChangeset.toTyped(readModelClass: KClass<T>): ReadModelChangeset<T> =
        ReadModelChangeset(
            namespace = namespace,
            modelKey = modelKey,
            readModel = if (removed || readModel.isNullOrBlank() || readModel == "null") {
                null
            } else {
                chronicleGson.fromJson(readModel, readModelClass.java)
            },
            removed = removed,
            changeType = changeType.toClient(),
            eventSequenceNumber = eventSequenceNumber,
            occurred = if (hasOccurred()) occurred.value.toInstantOrNull() else null,
            correlationId = if (hasCorrelationId()) correlationId.toUUID() else null
        )
}

private fun String.toInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (e: Exception) {
    null
}

private fun Readmodels.ReadModelChangeType.toClient(): ReadModelChangeType = when (this) {
    Readmodels.ReadModelChangeType.Added -> ReadModelChangeType.Added
    Readmodels.ReadModelChangeType.Removed -> ReadModelChangeType.Removed
    else -> ReadModelChangeType.Modified
}

/**
 * Converts a wire [Bcl.Guid] (lo/hi, little-endian halves) back to a Java [UUID] (big-endian halves) -
 * the inverse of the `UUID.toContractsGuid()` conversion used when appending events.
 */
private fun Bcl.Guid.toUUID(): UUID {
    val mostSignificantBits = java.lang.Long.reverseBytes(lo)
    val leastSignificantBits = java.lang.Long.reverseBytes(hi)
    return UUID(mostSignificantBits, leastSignificantBits)
}
