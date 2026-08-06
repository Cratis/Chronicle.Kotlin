// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.IEventTypeMigration
import io.cratis.chronicle.java.BlockingReactorMethodArgumentResolver
import io.cratis.chronicle.java.BlockingReactorMiddleware
import io.cratis.chronicle.observation.IReactorMethodArgumentResolver
import io.cratis.chronicle.observation.IReactorMiddleware
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.webhooks.IWebhookDefiner
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import io.github.classgraph.ScanResult
import kotlin.reflect.KClass

/**
 * Discovers Chronicle artifacts by scanning the classpath.
 *
 * Scanning happens once, on first access, and the result is kept for the lifetime of the instance —
 * so the cost is paid at startup and never again. Reach for [default] rather than constructing this
 * yourself unless the scan needs narrowing.
 *
 * Narrowing the scan to the packages an application actually owns makes startup faster and keeps
 * artifacts belonging to third-party libraries out of the picture:
 *
 * ```kotlin
 * val options = ChronicleOptions.development().copy(
 *     artifacts = ClientArtifacts("com.acme.ordering", "com.acme.shipping"))
 * ```
 *
 * @param packages Packages to scan, including sub-packages. Scans everything on the classpath when empty.
 * @param classLoaders Class loaders to scan through. Defaults to the current thread's context class loader.
 */
class ClientArtifacts(
    private val packages: List<String> = emptyList(),
    private val classLoaders: List<ClassLoader> = defaultClassLoaders()
) : IClientArtifacts {
    constructor(vararg packages: String) : this(packages.toList())

    private val discovered: Discovered by lazy { scan() }

    override val eventTypes: List<KClass<*>> get() = discovered.eventTypes
    override val eventTypeMigrations: List<KClass<*>> get() = discovered.eventTypeMigrations
    override val readModels: List<KClass<*>> get() = discovered.readModels
    override val projections: List<KClass<*>> get() = discovered.projections
    override val modelBoundProjections: List<KClass<*>> get() = discovered.modelBoundProjections
    override val reactors: List<KClass<*>> get() = discovered.reactors
    override val reducers: List<KClass<*>> get() = discovered.reducers
    override val constraints: List<KClass<*>> get() = discovered.constraints
    override val eventSeeders: List<KClass<*>> get() = discovered.eventSeeders
    override val webhooks: List<KClass<*>> get() = discovered.webhooks
    override val reactorMiddlewares: List<KClass<*>> get() = discovered.reactorMiddlewares
    override val reactorArgumentResolvers: List<KClass<*>> get() = discovered.reactorArgumentResolvers

    private fun scan(): Discovered = newClassGraph().scan().use { result ->
        Discovered(
            eventTypes = result.withAnnotation(EventType::class) { it.isEventType() },
            eventTypeMigrations = result.implementing(IEventTypeMigration::class) { it.isEventTypeMigration() },
            readModels = result.withAnnotation(ReadModel::class) { it.isReadModel() },
            projections = result.implementing(IProjectionFor::class) { it.isDeclarativeProjection() },
            // Kotlin compiles a repeatable annotation into a synthetic `Container` holding the
            // repeats, so a class carrying more than one @FromEvent is only annotated with the
            // container as far as the bytecode - and therefore the scanner - is concerned.
            modelBoundProjections = (
                result.withAnnotation(FromEvent::class) { it.isModelBoundProjection() } +
                    result.withAnnotationNamed(FROM_EVENT_CONTAINER) { it.isModelBoundProjection() }
                ).distinct(),
            reactors = result.withAnnotation(Reactor::class) { it.isReactor() },
            reducers = result.withAnnotation(Reducer::class) { it.isReducer() },
            constraints = result.implementing(IConstraint::class) { it.isConstraint() },
            eventSeeders = result.implementing(ICanSeedEvents::class) { it.isEventSeeder() },
            webhooks = result.implementing(IWebhookDefiner::class) { it.isWebhookDefiner() },
            reactorMiddlewares = (
                result.implementing(IReactorMiddleware::class) { it.isReactorMiddleware() } +
                    result.implementing(BlockingReactorMiddleware::class) { it.isReactorMiddleware() }
                ).distinct(),
            reactorArgumentResolvers = (
                result.implementing(IReactorMethodArgumentResolver::class) {
                    it.isReactorMethodArgumentResolver()
                } +
                    result.implementing(BlockingReactorMethodArgumentResolver::class) {
                        it.isReactorMethodArgumentResolver()
                    }
                ).distinct()
        )
    }

    private fun newClassGraph(): ClassGraph {
        val graph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .overrideClassLoaders(*classLoaders.toTypedArray())

        return if (packages.isEmpty()) {
            graph.rejectPackages(*IGNORED_PACKAGES)
        } else {
            graph.acceptPackages(*packages.toTypedArray())
        }
    }

    private fun ScanResult.withAnnotation(annotation: KClass<out Annotation>, qualifies: (KClass<*>) -> Boolean): List<KClass<*>> =
        withAnnotationNamed(annotation.java.name, qualifies)

    private fun ScanResult.withAnnotationNamed(name: String, qualifies: (KClass<*>) -> Boolean): List<KClass<*>> =
        getClassesWithAnnotation(name).qualifying(qualifies)

    private fun ScanResult.implementing(contract: KClass<*>, qualifies: (KClass<*>) -> Boolean): List<KClass<*>> =
        getClassesImplementing(contract.java.name).qualifying(qualifies)

    /**
     * Loads each scanned class and keeps only those the shared classification rules accept, so a
     * discovered artifact and a hand-listed one are always judged identically. A class that cannot be
     * loaded - a missing optional dependency, say - is skipped rather than failing the whole scan.
     */
    private fun ClassInfoList.qualifying(qualifies: (KClass<*>) -> Boolean): List<KClass<*>> =
        mapNotNull { info ->
            runCatching { info.loadClass().kotlin.takeIf(qualifies) }.getOrNull()
        }.distinct()

    /** The outcome of a single scan, so every artifact kind is answered from one pass over the classpath. */
    private class Discovered(
        val eventTypes: List<KClass<*>>,
        val eventTypeMigrations: List<KClass<*>>,
        val readModels: List<KClass<*>>,
        val projections: List<KClass<*>>,
        val modelBoundProjections: List<KClass<*>>,
        val reactors: List<KClass<*>>,
        val reducers: List<KClass<*>>,
        val constraints: List<KClass<*>>,
        val eventSeeders: List<KClass<*>>,
        val webhooks: List<KClass<*>>,
        val reactorMiddlewares: List<KClass<*>>,
        val reactorArgumentResolvers: List<KClass<*>>
    )

    companion object {
        /**
         * The classpath-wide [ClientArtifacts] used unless something else is configured.
         *
         * Shared across every event store in the process, so the classpath is scanned at most once
         * no matter how many event stores an application opens.
         */
        val default: IClientArtifacts by lazy { ClientArtifacts() }

        /** The synthetic holder Kotlin generates for repeats of [FromEvent] on a single class. */
        private const val FROM_EVENT_CONTAINER = "io.cratis.chronicle.projections.FromEvent\$Container"

        /**
         * Packages skipped when no explicit package list is given.
         *
         * None of them can hold application artifacts, and skipping them keeps a full-classpath scan
         * to a fraction of a second even in a large application.
         */
        private val IGNORED_PACKAGES = arrayOf(
            "java", "javax", "jakarta", "jdk", "sun", "com.sun",
            "kotlin", "kotlinx", "org.jetbrains",
            "com.google", "io.grpc", "io.netty", "io.perfmark", "io.opencensus", "io.github.classgraph",
            "org.apache", "org.slf4j", "ch.qos", "org.checkerframework", "org.codehaus",
            "org.springframework", "org.aopalliance", "org.yaml", "org.xmlunit",
            "org.junit", "org.opentest4j", "org.mockito", "net.bytebuddy", "io.mockk",
            "Cratis.Chronicle.Contracts"
        )
    }
}

/** The class loaders scanned when none are given — the context class loader, falling back to the client's own. */
internal fun defaultClassLoaders(): List<ClassLoader> =
    listOfNotNull(
        Thread.currentThread().contextClassLoader,
        ClientArtifacts::class.java.classLoader
    ).distinct()
