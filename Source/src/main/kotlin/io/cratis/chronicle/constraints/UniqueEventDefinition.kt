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
 * Not a data class on purpose: a data class publishes `copy` and `componentN`, which would bypass the
 * validation below and freeze the constructor shape into the public API.
 *
 * @param eventClass The event type the definition applies to.
 * @param properties The names of the properties, on [eventClass], that together must be unique. There must
 * be at least one and no name may repeat - the kernel keys a constraint's values by property name. The list
 * is copied, so changing the one passed in afterwards does not change this definition.
 * @throws IllegalArgumentException if [properties] is empty or names a property more than once.
 */
class UniqueEventDefinition(
    val eventClass: KClass<*>,
    properties: List<String>
) {
    val properties: List<String> = properties.toList()

    init {
        require(this.properties.isNotEmpty()) {
            "A unique constraint on '${eventClass.simpleName}' must name at least one property."
        }
        require(this.properties.distinct().size == this.properties.size) {
            "A unique constraint on '${eventClass.simpleName}' names the same property more than once: ${this.properties}."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is UniqueEventDefinition && eventClass == other.eventClass && properties == other.properties

    override fun hashCode(): Int = 31 * eventClass.hashCode() + properties.hashCode()

    override fun toString(): String = "UniqueEventDefinition(eventClass=$eventClass, properties=$properties)"
}
