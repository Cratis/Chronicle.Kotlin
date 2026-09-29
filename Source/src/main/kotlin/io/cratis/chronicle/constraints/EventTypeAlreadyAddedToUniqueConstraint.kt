// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

/**
 * Thrown when the same event type is added to one unique constraint more than once.
 *
 * Name every property of an event type in a single `on(...)` call - a second call for the same event
 * type would otherwise leave it ambiguous which properties the constraint is meant to cover.
 *
 * @param eventClass The event type that was added more than once.
 * @param properties The properties in the definition that was rejected.
 */
class EventTypeAlreadyAddedToUniqueConstraint(
    val eventClass: KClass<*>,
    val properties: List<String>
) : IllegalStateException(
    "The event type '${eventClass.simpleName}' with properties '${properties.joinToString(", ")}' has already " +
        "been added to the unique constraint. List every property of an event type in a single on(...) call."
)
