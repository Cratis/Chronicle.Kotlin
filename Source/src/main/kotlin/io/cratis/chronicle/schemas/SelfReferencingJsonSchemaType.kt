// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import kotlin.reflect.KClass

/**
 * Thrown when a type adorned with [JsonSchemaType] points at itself.
 *
 * Generating the schema for such a type would recurse forever, so it is rejected up front with a
 * message naming the offending type rather than being left to overflow the stack.
 *
 * @param type The type that represents itself.
 */
class SelfReferencingJsonSchemaType(type: KClass<*>) : Exception(
    "'${type.qualifiedName ?: type.java.name}' is adorned with @JsonSchemaType pointing at itself. " +
        "Point it at the type its converter actually produces."
)
