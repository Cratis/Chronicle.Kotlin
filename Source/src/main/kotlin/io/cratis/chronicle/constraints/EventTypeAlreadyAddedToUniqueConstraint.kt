// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

/**
 * Thrown when the same event type is added to one unique constraint more than once.
 *
 * With the fluent builder, name every property of an event type in a single `on(...)` call - a second
 * call for the same event type would otherwise leave it ambiguous which properties the constraint is
 * meant to cover. With model-bound [Unique] annotations, it is thrown at registration when two
 * properties of one event type share a constraint name, because the kernel cannot resolve a constraint
 * that lists the same event type twice.
 *
 * @property eventClass The event type that was added more than once.
 * @property properties The properties in the definition that was rejected.
 * @property constraintName The name of the constraint, or `null` when the fluent builder rejected it
 *   before the constraint had a name.
 */
class EventTypeAlreadyAddedToUniqueConstraint private constructor(
    val eventClass: KClass<*>,
    val properties: List<String>,
    val constraintName: String?,
    message: String
) : IllegalStateException(message) {

    /**
     * Initializes a new instance for an event type rejected by the fluent builder.
     *
     * @param eventClass The event type that was added more than once.
     * @param properties The properties in the definition that was rejected.
     */
    constructor(eventClass: KClass<*>, properties: List<String>) : this(
        eventClass,
        properties,
        null,
        "The event type '${eventClass.simpleName}' with properties '${properties.joinToString(", ")}' has already " +
            "been added to the unique constraint. List every property of an event type in a single on(...) call."
    )

    /**
     * Initializes a new instance for an event type rejected from a model-bound constraint.
     *
     * @param constraintName The name of the constraint that lists the event type more than once.
     * @param eventClass The event type that was added more than once.
     * @param properties The properties of the event type that share the constraint name.
     */
    constructor(constraintName: String, eventClass: KClass<*>, properties: List<String>) : this(
        eventClass,
        properties,
        constraintName,
        "The event type '${eventClass.simpleName}' with properties '${properties.joinToString(", ")}' has already " +
            "been added to the unique constraint with name '$constraintName'. Give each [Unique] property of an " +
            "event type its own id."
    )
}
