// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

/**
 * One event type taking part in a unique constraint, together with the properties that make it unique.
 *
 * A unique constraint is made of one or more of these. Within a single definition the [properties]
 * together form the unique value - `firstName` and `lastName` are unique as a pair, not each on their own.
 *
 * @param eventClass The event type the definition applies to.
 * @param properties The names of the properties, on [eventClass], that together must be unique.
 */
data class UniqueEventDefinition(
    val eventClass: KClass<*>,
    val properties: List<String>
) {
    init {
        require(properties.isNotEmpty()) {
            "A unique constraint on '${eventClass.simpleName}' must name at least one property."
        }
    }
}
