// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.observation.EventSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * What the [Projection] annotation contributes to a projection definition, read off the annotated class.
 *
 * Both projection styles land here: for a declarative projection the annotated class is the
 * [IProjectionFor] implementation, and for a model-bound projection it is the read model itself.
 *
 * @property id The projection identifier, defaulting to the class simple name.
 * @property eventSequenceId The event sequence to observe, defaulting to the event log.
 */
internal data class ProjectionRegistration(
    val id: String,
    val eventSequenceId: String
) {
    companion object {
        /** Reads [projectionClass]'s [Projection] annotation, which is optional. */
        fun from(projectionClass: KClass<*>): ProjectionRegistration {
            val annotation = projectionClass.findAnnotation<Projection>()

            return ProjectionRegistration(
                id = annotation?.id?.ifEmpty { null } ?: projectionClass.simpleName!!,
                eventSequenceId = EventSequence.idOf(projectionClass, annotation?.eventSequence)
            )
        }
    }
}
