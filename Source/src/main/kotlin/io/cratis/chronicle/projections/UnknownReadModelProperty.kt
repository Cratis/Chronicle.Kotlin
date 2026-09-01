// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when the declarative projection builder is configured with a property name that does not
 * exist on the target type.
 *
 * The property-name overloads on the declarative builder exist so a Java caller - who cannot produce
 * a [kotlin.reflect.KProperty1] - can use the same explicit mapping API a Kotlin caller reaches for
 * with a property reference. That convenience trades away the compiler's guarantee that the name is
 * real, so it is checked here, at the point the builder is configured, rather than surfacing later as
 * a projection definition the kernel rejects.
 *
 * @param type The type [propertyName] was expected to be found on.
 * @param propertyName The property name that does not exist on [type].
 */
class UnknownReadModelProperty(
    type: KClass<*>,
    propertyName: String
) : IllegalArgumentException(
    "'$propertyName' is not a property of '${type.simpleName}'."
)
