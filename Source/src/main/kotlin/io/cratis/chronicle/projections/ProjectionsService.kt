// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.artifacts.isGlobalForHandler
import io.cratis.chronicle.artifacts.isVariant
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.projections.graphs.GraphProjections
import io.cratis.chronicle.projections.graphs.IGraphProjection
import io.cratis.chronicle.projections.graphs.isGraphElement
import io.cratis.chronicle.projections.graphs.isGraphView
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.readModels.readModelIdentifier
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.grpc.Status
import kotlinx.coroutines.delay
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The definition built for one read model class, plus what it declared about being a variant - carried
 * from the point it was built to the cross-wiring pass in [ProjectionsService.register] that runs once
 * every variant of a group has been built.
 */
private data class BuiltDefinition(
    val readModelClass: KClass<*>,
    val definition: ProjectionsOuterClass.ProjectionDefinition,
    val declaration: VariantDeclaration? = null
)

/** The literal key value meaning "correlate on the event source id", matching the kernel's key convention. */
internal const val EVENT_SOURCE_ID_KEY = "EventSourceId"
internal const val EVENT_SOURCE_ID_EXPRESSION = "\$eventSourceId"

private val FIRST_STATUS_DELAY = 50.milliseconds
private val MAX_STATUS_DELAY = 2.seconds

internal fun wireKey(key: String): String = if (key == EVENT_SOURCE_ID_KEY) EVENT_SOURCE_ID_EXPRESSION else key

class ProjectionsService(
    private val eventStoreName: String,
    private val stub: ProjectionsGrpcKt.ProjectionsCoroutineStub,
    private val readModels: ReadModelsService,
    private val namespace: String = io.cratis.chronicle.EventStoreNamespaceName.default.value,
    private val registrant: String = ""
) : IProjectionsService {
    /** How long a registration waits for the kernel to apply it, or for a busy kernel to take it in, before it stops waiting. */
    internal var maxWaitForRegistration: Duration = 5.minutes

    /** How a wait between two questions to the kernel is done. */
    internal var waitForKernel: suspend (Duration) -> Unit = { delay(it) }

    /** The sink that stores the graphs this service registers, from [io.cratis.chronicle.ChronicleOptions.defaultGraphSinkTypeId]. */
    internal var graphSinkTypeId: String = WellKnownSinkTypes.NEO4J

    override suspend fun query(declaration: String, eventSequenceId: EventSequenceId): ProjectionQueryResult {
        val request = ProjectionsOuterClass.PreviewProjectionRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setEventSequenceId(eventSequenceId.value)
            .setDeclaration(declaration)
            .build()

        val response = stub.preview(request)

        // The kernel answers with one of two things. A declaration it could not parse is an ordinary
        // outcome of asking a question in a language, not an exception - the errors are what you show
        // whoever wrote it.
        return if (response.hasValue1() && response.value1.errorsCount > 0) {
            ProjectionQueryResult.Invalid(
                response.value1.errorsList.map {
                    ProjectionDeclarationError(it.message, it.line, it.column)
                }
            )
        } else {
            ProjectionQueryResult.Projected(response.value0.readModelEntriesList.toList())
        }
    }

    override suspend fun register(vararg projections: Any) {
        val modelBoundClasses = projections.filterIsInstance<KClass<*>>()
        // A GlobalFor handler is discovered so it can be found here, but it is never a projection in
        // its own right - it exists purely to be merged into every variant of its identity, below.
        val globalHandlersByIdentity: Map<KClass<*>, List<KClass<*>>> = modelBoundClasses
            .filter { it.isGlobalForHandler() }
            .groupBy { it.findAnnotation<GlobalFor>()!!.identity }

        val built = mutableListOf<BuiltDefinition>()
        for (projection in projections) {
            when {
                // A property graph is registered with its own definitions, below - a node that carries
                // FromEvent is not a model-bound read model.
                projection is KClass<*> && projection.isGraphElement() -> Unit
                projection is IGraphProjection -> Unit
                // A view reads a graph; it is checked with the graphs, below, and registers nothing itself.
                projection is KClass<*> && projection.isGraphView() -> Unit
                projection is KClass<*> && projection.isGlobalForHandler() && !projection.isVariant() -> Unit
                projection is KClass<*> -> {
                    val globalHandlerClasses = projection.findAnnotation<VariantOf>()
                        ?.let { globalHandlersByIdentity[it.identity] }
                        .orEmpty()
                    buildModelBoundDefinition(projection, globalHandlerClasses)?.let { built.add(it) }
                }
                projection is IProjectionFor<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    buildDeclarativeDefinition(projection as IProjectionFor<Any>)?.let { built.add(it) }
                }
                else -> Unit
            }
        }
        val declared = GraphProjections(graphSinkTypeId).declarationsFor(projections.toList())
        val graphs = declared.registrations
        if (built.isEmpty() && graphs.isEmpty()) {
            declared.throwIfAnyViewIsInvalid()
            return
        }

        val definitionsByReadModel = built.associate { it.readModelClass to it.definition }
        val declarations = built.mapNotNull { b -> b.declaration?.let { b.readModelClass to it } }.toMap()
        val crossWired = VariantReclassifier.crossWireGroups(definitionsByReadModel, declarations)

        // The read model a graph is stored as has to exist before the projection that fills it.
        readModels.registerDefinitions(graphs.map { it.readModel })

        val request = ProjectionsOuterClass.RegisterRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addAllProjections(built.map { crossWired.getValue(it.readModelClass) })
            .addAllProjections(graphs.map { it.projection })
            // Naming who registers is what lets the kernel report when it has applied what it accepted. It is not
            // the full set: a registration made by hand says nothing about the projections the client does not name.
            .setRegistrant(registrant)
            .build()

        registerWithKernel(request)
        waitUntilApplied()

        // An invalid view does not keep anything else from registering; it is reported once everything is.
        declared.throwIfAnyViewIsInvalid()
    }

    /**
     * Sends the registration, waiting out a kernel that says it is busy.
     *
     * A busy kernel answers UNAVAILABLE with a `retry-after` trailer, in seconds; that is when to ask again.
     */
    private suspend fun registerWithKernel(request: ProjectionsOuterClass.RegisterRequest) {
        val started = TimeSource.Monotonic.markNow()
        while (true) {
            try {
                stub.register(request)
                return
            } catch (error: Exception) {
                val retryAfter = error.retryAfter() ?: throw error
                if (started.elapsedNow() + retryAfter > maxWaitForRegistration) throw error
                waitForKernel(retryAfter)
            }
        }
    }

    /**
     * Waits until the kernel has applied what it accepted from this registrant.
     *
     * The kernel takes a registration in and answers at once; applying it happens in the background. Asking with short
     * calls rather than one long one is what keeps a busy kernel from timing the client out. A kernel that applies
     * before it answers has no status to ask for, and answers UNIMPLEMENTED or never says pending.
     *
     * @throws ProjectionRegistrationNotApplied When the kernel accepted the registration but could not apply it.
     */
    private suspend fun waitUntilApplied() {
        if (registrant.isEmpty()) return

        val request = ProjectionsOuterClass.GetRegistrationStatusRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setRegistrant(registrant)
            .build()
        val started = TimeSource.Monotonic.markNow()
        var delay = FIRST_STATUS_DELAY
        while (true) {
            val status = try {
                stub.getRegistrationStatus(request)
            } catch (error: Exception) {
                if (error.grpcStatusCode() == Status.Code.UNIMPLEMENTED) return
                throw error
            }

            when (status.state) {
                ProjectionsOuterClass.RegistrationState.Applied -> return
                ProjectionsOuterClass.RegistrationState.Failed -> throw ProjectionRegistrationNotApplied(registrant, status.failure)
                else -> Unit
            }

            if (started.elapsedNow() > maxWaitForRegistration) {
                System.err.println("[ProjectionsService] '$registrant' is still being applied after $maxWaitForRegistration; no longer waiting")
                return
            }
            waitForKernel(delay)
            delay = minOf(delay * 2, MAX_STATUS_DELAY)
        }
    }

    /**
     * Builds a projection definition from a class that implements [IProjectionFor].
     * The [Projection] annotation is optional — when absent the class simple name is used as the identifier.
     * The read model type is inferred from the [IProjectionFor] type parameter.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun buildDeclarativeDefinition(projection: IProjectionFor<Any>): BuiltDefinition? {
        val projectionClass = projection::class
        val registration = ProjectionRegistration.from(projectionClass)
        val projectionId = registration.id

        val readModelClass = projectionClass.supertypes
            .firstOrNull { it.classifier?.toString()?.contains("IProjectionFor") == true }
            ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
            ?: return null

        val builderFor = ProjectionBuilderFor(readModelClass as KClass<Any>)
        projection.define(builderFor)

        var fromPairs = builderFor.fromEntries.mapNotNull { entry ->
            buildFromPair(entry.eventClass, entry.key, entry.properties)
        }
        var joinPairs = buildJoinPairsFromEntries(builderFor.joinEntries)
        var declaration: VariantDeclaration? = null

        val variantIdentity = builderFor.variantIdentity
        if (variantIdentity != null) {
            val enteringEventTypes = builderFor.enteringEventClasses.mapNotNull { eventClass ->
                val eventAnnotation = eventClass.findAnnotation<io.cratis.chronicle.events.EventType>() ?: return@mapNotNull null
                val eventTypeId = eventAnnotation.id.ifEmpty { eventClass.simpleName!! }
                toWireEventType(eventTypeId, eventAnnotation.generation)
            }
            val (reclassifiedFrom, reclassifiedJoin) = VariantReclassifier.reclassify(
                readModelClass,
                fromPairs,
                joinPairs,
                enteringEventTypes,
                builderFor.variantKey
            )
            fromPairs = reclassifiedFrom
            joinPairs = reclassifiedJoin
            declaration = VariantDeclaration(readModelClass, variantIdentity, enteringEventTypes)
        }

        readModels.registerWithObserver(readModelClass, 2, projectionId)

        val definition = buildProjectionDefinition(
            projectionId,
            registration.eventSequenceId,
            readModelClass,
            fromPairs,
            joinPairs = joinPairs,
            children = buildChildrenMapFromEntries(builderFor.childrenEntries, builderFor.childrenOrderings),
            nested = buildNestedMapFromEntries(builderFor.nestedEntries),
            isRewindable = builderFor.isRewindable,
            isActive = readModelClass.findAnnotation<Passive>() == null,
            autoMapEnabled = builderFor.autoMapEnabled,
            removedWith = buildRemovedWithPairsFromEntries(builderFor.removedWithEntries),
            removedWithJoin = buildRemovedWithJoinPairsFromEntries(builderFor.removedWithJoinEntries),
            all = buildFromEveryDefinitionFromEntries(builderFor.fromEveryProperties)
        )
        return BuiltDefinition(readModelClass, definition, declaration)
    }

    /**
     * Builds a projection definition from a read model class annotated with [FromEvent] and/or
     * [VariantOf]. The projection identifier defaults to the class simple name; use [Projection] on
     * the class to override it (e.g. after a rename).
     * Property mappings come from [SetFrom]/[SetFromContext]/[SetValue] annotations on individual
     * properties; structural shape comes from [Join], [ChildrenFrom] and [Nested]/[ClearWith].
     *
     * @param globalHandlerClasses Every [GlobalFor] type sharing this class's [VariantOf] identity,
     *   when it has one. Ignored otherwise.
     */
    private suspend fun buildModelBoundDefinition(
        readModelClass: KClass<*>,
        globalHandlerClasses: List<KClass<*>> = emptyList()
    ): BuiltDefinition? {
        val fromEventAnnotations = readModelClass.findAnnotations<FromEvent>()
        val variantAnnotation = readModelClass.findAnnotation<VariantOf>()
        if (fromEventAnnotations.isEmpty() && variantAnnotation == null) return null

        val registration = ProjectionRegistration.from(readModelClass)
        val projectionId = registration.id

        var fromPairs = fromEventAnnotations.mapNotNull { fromAnn ->
            val mapped = buildPropertyMappingsForEvent(readModelClass, fromAnn.eventType)
            buildFromPair(fromAnn.eventType, mapped.resolvedKey(fromAnn.key), mapped.properties)
        }
        var joinPairs = collectJoinPairs(readModelClass)
        var declaration: VariantDeclaration? = null

        if (variantAnnotation != null) {
            val enteringEventTypes = VariantReclassifier.enteringEventTypesFrom(readModelClass)
            // The entering event is what creates the variant, so it must have a From even when the
            // author maps no properties on it explicitly (no matching @SetFrom) and leaves the
            // mapping to AutoMap.
            val declaredEventTypes = fromEventAnnotations.map { it.eventType }.toSet()
            val implicitFromPairs = readModelClass.findAnnotations<EntersOn>()
                .map { it.eventType }
                .filter { it !in declaredEventTypes }
                .mapNotNull { buildFromPair(it, "EventSourceId", emptyMap()) }
            fromPairs = fromPairs + implicitFromPairs

            val globalHandlerFromPairs = globalHandlerClasses.associateWith { handlerClass ->
                handlerClass.findAnnotations<FromEvent>().mapNotNull { fromAnn ->
                    val mapped = buildPropertyMappingsForEvent(handlerClass, fromAnn.eventType)
                    buildFromPair(fromAnn.eventType, mapped.resolvedKey(fromAnn.key), mapped.properties)
                }
            }
            fromPairs = VariantReclassifier.mergeGlobalHandlers(readModelClass, fromPairs, globalHandlerFromPairs)

            val (reclassifiedFrom, reclassifiedJoin) = VariantReclassifier.reclassify(
                readModelClass,
                fromPairs,
                joinPairs,
                enteringEventTypes,
                variantAnnotation.key
            )
            fromPairs = reclassifiedFrom
            joinPairs = reclassifiedJoin
            declaration = VariantDeclaration(readModelClass, variantAnnotation.identity, enteringEventTypes)
        }

        readModels.registerWithObserver(readModelClass, 2, projectionId)

        val definition = buildProjectionDefinition(
            projectionId,
            registration.eventSequenceId,
            readModelClass,
            fromPairs,
            joinPairs = joinPairs,
            children = collectChildrenMap(readModelClass),
            nested = collectNestedMap(readModelClass),
            isRewindable = readModelClass.findAnnotation<NotRewindable>() == null,
            isActive = readModelClass.findAnnotation<Passive>() == null,
            autoMapEnabled = readModelClass.findAnnotation<NoAutoMap>() == null,
            noAutoMapProperties = NoAutoMapProperties.collectFrom(readModelClass),
            removedWith = buildRemovedWithPairs(readModelClass.findAnnotations<RemovedWith>()),
            removedWithJoin = buildRemovedWithJoinPairs(readModelClass.findAnnotations<RemovedWithJoin>()),
            all = collectFromEveryDefinition(readModelClass)
        )
        return BuiltDefinition(readModelClass, definition, declaration)
    }

    private fun buildProjectionDefinition(
        projectionId: String,
        eventSequenceId: String,
        readModelClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>,
        joinPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> = emptyList(),
        children: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap(),
        nested: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap(),
        isRewindable: Boolean = true,
        isActive: Boolean = true,
        autoMapEnabled: Boolean = true,
        noAutoMapProperties: List<String> = emptyList(),
        removedWith: List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition> = emptyList(),
        removedWithJoin: List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = emptyList(),
        all: ProjectionsOuterClass.FromEveryDefinition? = null
    ): ProjectionsOuterClass.ProjectionDefinition {
        val readModelName = readModelClass.readModelIdentifier()
        val initialModelStateJson = try {
            val ctor = readModelClass.primaryConstructor
            if (ctor != null && ctor.parameters.all { it.isOptional }) {
                chronicleGson.toJson(ctor.callBy(emptyMap()))
            } else {
                "{}"
            }
        } catch (_: Exception) {
            "{}"
        }

        val builder = ProjectionsOuterClass.ProjectionDefinition.newBuilder()
            .setIdentifier(projectionId)
            .setReadModel(readModelName)
            .setInitialModelState(initialModelStateJson)
            .setEventSequenceId(eventSequenceId)
            .setIsActive(isActive)
            .setIsRewindable(isRewindable)
            .addAllFrom(fromPairs)
            .addAllJoin(joinPairs)
            .putAllChildren(children)
            .putAllNested(nested)
            .addAllRemovedWith(removedWith)
            .addAllRemovedWithJoin(removedWithJoin)
            .addAllNoAutoMapProperties(noAutoMapProperties)
            .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
        if (all != null) builder.setAll(all)
        return builder.build()
    }
}
