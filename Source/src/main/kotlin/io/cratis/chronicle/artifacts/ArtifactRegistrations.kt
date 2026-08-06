// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Registers every artifact an application owns with the kernel, in the order the kernel needs them.
 *
 * Order is not cosmetic. Event types come first because everything else refers to them; read models
 * follow so observers have somewhere to write; observers are registered before seeding so that seeded
 * events are never appended to a store nobody is watching yet.
 *
 * Calling [registerAll] more than once is safe and expected — the client calls it again on every
 * reconnect, since a kernel that restarted has forgotten the declarations made to it. Reactors and
 * reducers are started only on the first pass: each one already re-establishes its own observation
 * whenever the connection comes back.
 *
 * @param eventStore The event store to register into.
 * @param artifacts The artifacts to register.
 * @param activator Creates the instances for artifacts registered as objects rather than as classes.
 */
class ArtifactRegistrations(
    private val eventStore: IEventStore,
    private val artifacts: IClientArtifacts,
    private val activator: IArtifactActivator = ArtifactActivator
) {
    private val mutex = Mutex()
    private val initial = CompletableDeferred<Unit>()
    private var observersStarted = false

    /** Completes once the first full registration pass has finished. */
    val completed: Deferred<Unit> get() = initial

    /** Registers everything with the kernel. */
    suspend fun registerAll() = mutex.withLock {
        // Event types have to exist before anything that refers to them - observers, constraints and
        // projections are all expressed in terms of event type ids. Migrations ride along in the same
        // call: the event types service merges them into the registration for the type they migrate.
        eventStore.eventTypes.register(*(artifacts.eventTypes + artifacts.eventTypeMigrations).toTypedArray())

        // Reducers and projections register their own read models, tagged with the observer that
        // produces them. Registering those here as well would overwrite that with "no observer", so
        // only the read models nobody produces are registered directly.
        eventStore.readModels.register(*unownedReadModels().toTypedArray())

        eventStore.constraints.register(*instancesOf(artifacts.constraints).toTypedArray())
        // Projections come in two shapes: a class that defines one through a builder, registered as an
        // instance, and a read model that declares the events it projects from, registered as a class.
        val projections: List<Any> = instancesOf(artifacts.projections) + artifacts.modelBoundProjections
        eventStore.projections.register(*projections.toTypedArray())
        eventStore.webhooks.register(*instancesOf(artifacts.webhooks).toTypedArray())

        if (!observersStarted) {
            observersStarted = true
            artifacts.reactors.forEach { eventStore.reactors.register(activator.activate(it)) }
            artifacts.reducers.forEach { eventStore.reducers.register(activator.activate(it)) }
        }

        // Seeded events are appended by the kernel the moment the seed lands, so every observer that
        // should see them has to be registered by now.
        eventStore.seeding.seed(*instancesOf(artifacts.eventSeeders).toTypedArray())

        initial.complete(Unit)
        Unit
    }

    /**
     * The read models no reducer or projection produces.
     *
     * A read model that *is* produced by one is registered by that observer instead, which is the only
     * place the observer type and identifier are known.
     */
    private fun unownedReadModels(): List<KClass<*>> {
        val owned = buildSet {
            artifacts.reducers.mapNotNullTo(this) { it.reducedReadModel() }
            artifacts.projections.mapNotNullTo(this) { it.projectedReadModel() }
            addAll(artifacts.modelBoundProjections)
        }
        return artifacts.readModels.filterNot { it in owned }
    }

    private fun instancesOf(types: List<KClass<*>>): List<Any> =
        types.map { activator.activate(it) }

    /**
     * The read model a reducer folds events into, taken from the return type of its first handler —
     * the same rule the reducers service itself applies when registering the reducer.
     */
    private fun KClass<*>.reducedReadModel(): KClass<*>? =
        memberFunctions
            .firstOrNull { fn ->
                val event = fn.parameters.getOrNull(1)?.type?.classifier as? KClass<*>
                event?.hasAnnotation<EventType>() == true
            }
            ?.returnType?.classifier as? KClass<*>

    /** The read model a declarative projection targets, taken from its [IProjectionFor] type argument. */
    private fun KClass<*>.projectedReadModel(): KClass<*>? =
        supertypes
            .firstOrNull { it.classifier == IProjectionFor::class }
            ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
}
