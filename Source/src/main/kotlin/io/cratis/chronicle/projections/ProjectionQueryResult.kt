// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.json.chronicleGson
import kotlin.reflect.KClass

/**
 * What came back from running a projection declaration through [IProjectionsService.query].
 *
 * A declaration is a piece of text, so the first thing that can go wrong is that the kernel cannot
 * parse it. That is not an exception - it is an ordinary outcome of asking a question in a language,
 * and the errors are what you show the person who wrote it - so it is one of the two shapes here
 * rather than something thrown.
 */
sealed class ProjectionQueryResult {
    /** Whether the declaration parsed and produced results. */
    abstract val isSuccess: Boolean

    /**
     * The declaration ran, and here is what it projected.
     *
     * @property entries One JSON document per read model instance the projection produced.
     */
    data class Projected(val entries: List<String>) : ProjectionQueryResult() {
        override val isSuccess: Boolean get() = true

        /**
         * The entries deserialized into [readModelClass].
         *
         * An ad-hoc declaration has no registered read model type, so the shape is whatever the
         * declaration said it was. Pass a class matching that shape - typically one written for the
         * query - and the client deserializes with the same serializer it uses everywhere else.
         *
         * @param readModelClass The class to deserialize each entry into.
         * @return One instance per entry, in the order the kernel produced them.
         */
        fun <T : Any> instancesOf(readModelClass: KClass<T>): List<T> =
            entries.map { chronicleGson.fromJson(it, readModelClass.java) }
    }

    /**
     * The kernel could not parse the declaration, and nothing ran.
     *
     * @property errors What is wrong with it, each naming a line and column.
     */
    data class Invalid(val errors: List<ProjectionDeclarationError>) : ProjectionQueryResult() {
        override val isSuccess: Boolean get() = false

        /** The errors as one message, for a log line or an exception. */
        override fun toString(): String =
            "The projection declaration could not be parsed: " + errors.joinToString("; ")
    }
}
