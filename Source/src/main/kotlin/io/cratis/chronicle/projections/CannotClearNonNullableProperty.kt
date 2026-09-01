// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when [SetValue] declares a clear for a property that cannot hold null.
 *
 * Clearing means returning a property to no value. A property declared non-nullable has no such
 * state, so writing its type's default - an empty string, a zero - would be a different fact the read
 * model cannot tell apart from a real value. The declaration is rejected rather than reinterpreted.
 *
 * @param type The read model type declaring the property.
 * @param propertyName The name of the property the clear was declared for.
 */
class CannotClearNonNullableProperty(type: KClass<*>, propertyName: String) : IllegalArgumentException(
    "Property '$propertyName' on '${type.qualifiedName ?: type.java.name}' is not nullable, so it cannot be " +
        "cleared. Declare the property as nullable, or use @SetValue(value = ...) with the value you actually " +
        "want it to hold."
)
