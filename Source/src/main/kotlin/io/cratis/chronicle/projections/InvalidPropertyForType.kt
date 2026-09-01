// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when a model-bound projection annotation names a property that does not exist on the type
 * it is checked against.
 *
 * Kotlin has no compile-time check tying a projection annotation's string property name to a real
 * member - the annotation happily accepts any string, forwards it to the kernel as-is, and a typo
 * only surfaces once the projection runs and silently yields no value for it. Failing at registration
 * turns that into an obvious startup error instead.
 *
 * @param type The type the property was expected to be on.
 * @param propertyName The property name that was not found.
 */
class InvalidPropertyForType(type: KClass<*>, propertyName: String) : IllegalArgumentException(
    "Property '$propertyName' does not exist on type '${type.qualifiedName ?: type.java.name}'."
)
