// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import io.cratis.chronicle.captures.ICapture
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
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

/**
 * The rules that decide which kind of Chronicle artifact a class is.
 *
 * Both [ClientArtifacts] (which scans) and [KnownClientArtifacts] (which is told) classify through
 * these, so a class discovered automatically and the same class listed by hand always land in
 * exactly the same buckets.
 */

/** Whether this class can be instantiated as an artifact — a concrete class, not an interface or an abstract one. */
internal fun KClass<*>.isInstantiableArtifact(): Boolean =
    !java.isInterface && !java.isAnnotation && !java.isEnum && !Modifier.isAbstract(java.modifiers)

/** Whether this class is an event type. */
internal fun KClass<*>.isEventType(): Boolean = hasAnnotation<EventType>()

/** Whether this class describes a migration between two generations of an event type. */
internal fun KClass<*>.isEventTypeMigration(): Boolean =
    isInstantiableArtifact() && isSubclassOf(IEventTypeMigration::class)

/** Whether this class is a read model. */
internal fun KClass<*>.isReadModel(): Boolean = hasAnnotation<ReadModel>()

/** Whether this class is a declarative projection — one that defines itself through a builder. */
internal fun KClass<*>.isDeclarativeProjection(): Boolean =
    isInstantiableArtifact() && isSubclassOf(IProjectionFor::class)

/** Whether this class is a model-bound projection — a read model that declares the events it projects from. */
internal fun KClass<*>.isModelBoundProjection(): Boolean = findAnnotations<FromEvent>().isNotEmpty()

/** Whether this class is a reactor. */
internal fun KClass<*>.isReactor(): Boolean = isInstantiableArtifact() && hasAnnotation<Reactor>()

/** Whether this class is a reducer. */
internal fun KClass<*>.isReducer(): Boolean = isInstantiableArtifact() && hasAnnotation<Reducer>()

/**
 * Whether this class wraps reactor handler invocations.
 *
 * Java cannot implement the suspending [IReactorMiddleware], so a Java middleware implements
 * [BlockingReactorMiddleware] instead and is adapted onto it - both count as the same kind here.
 */
internal fun KClass<*>.isReactorMiddleware(): Boolean =
    isInstantiableArtifact() &&
        (isSubclassOf(IReactorMiddleware::class) || isSubclassOf(BlockingReactorMiddleware::class))

/**
 * Whether this class supplies a reactor handler parameter past the event.
 *
 * As with middlewares, Java cannot implement the suspending contract, so a Java resolver implements
 * [BlockingReactorMethodArgumentResolver] instead and is adapted onto it.
 */
internal fun KClass<*>.isReactorMethodArgumentResolver(): Boolean =
    isInstantiableArtifact() &&
        (
            isSubclassOf(IReactorMethodArgumentResolver::class) ||
                isSubclassOf(BlockingReactorMethodArgumentResolver::class)
            )

/** Whether this class is a constraint. */
internal fun KClass<*>.isConstraint(): Boolean =
    isInstantiableArtifact() && isSubclassOf(IConstraint::class)

/** Whether this class seeds events. */
internal fun KClass<*>.isEventSeeder(): Boolean =
    isInstantiableArtifact() && isSubclassOf(ICanSeedEvents::class)

/** Whether this class declares a capture. */
internal fun KClass<*>.isCapture(): Boolean =
    isInstantiableArtifact() && isSubclassOf(ICapture::class)

/** Whether this class defines a webhook. */
internal fun KClass<*>.isWebhookDefiner(): Boolean =
    isInstantiableArtifact() && isSubclassOf(IWebhookDefiner::class)
