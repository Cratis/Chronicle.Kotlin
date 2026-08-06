// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * An [IClientArtifacts] built from an explicit list of classes rather than by scanning.
 *
 * Every class is sorted into the artifact kinds it qualifies for using the same rules
 * [ClientArtifacts] applies, so a class only has to be listed once no matter how many kinds it
 * belongs to — a `@ReadModel` carrying `@FromEvent` lands in both [readModels] and
 * [modelBoundProjections].
 *
 * Use this when classpath scanning is unwanted (a locked-down runtime, a native image, or a test
 * that must not see the rest of the classpath) but automatic registration still is.
 *
 * @param classes The classes making up the application's artifacts.
 */
class KnownClientArtifacts(classes: Iterable<KClass<*>>) : IClientArtifacts {
    constructor(vararg classes: KClass<*>) : this(classes.toList())

    private val candidates = classes.distinct()

    override val eventTypes: List<KClass<*>> = candidates.filter { it.isEventType() }
    override val eventTypeMigrations: List<KClass<*>> = candidates.filter { it.isEventTypeMigration() }
    override val readModels: List<KClass<*>> = candidates.filter { it.isReadModel() }
    override val projections: List<KClass<*>> = candidates.filter { it.isDeclarativeProjection() }
    override val modelBoundProjections: List<KClass<*>> = candidates.filter { it.isModelBoundProjection() }
    override val reactors: List<KClass<*>> = candidates.filter { it.isReactor() }
    override val reducers: List<KClass<*>> = candidates.filter { it.isReducer() }
    override val constraints: List<KClass<*>> = candidates.filter { it.isConstraint() }
    override val eventSeeders: List<KClass<*>> = candidates.filter { it.isEventSeeder() }
    override val webhooks: List<KClass<*>> = candidates.filter { it.isWebhookDefiner() }
    override val captures: List<KClass<*>> = candidates.filter { it.isCapture() }
    override val reactorMiddlewares: List<KClass<*>> = candidates.filter { it.isReactorMiddleware() }
    override val reactorArgumentResolvers: List<KClass<*>> =
        candidates.filter { it.isReactorMethodArgumentResolver() }

    companion object {
        /** An [IClientArtifacts] holding nothing — automatic registration then has nothing to register. */
        val empty: IClientArtifacts = KnownClientArtifacts(emptyList())
    }
}
