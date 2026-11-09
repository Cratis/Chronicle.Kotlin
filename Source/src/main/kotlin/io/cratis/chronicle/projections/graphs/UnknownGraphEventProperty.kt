// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when an edge is identified by a property and an event neither names the event property holding
 * the identity nor has a property named like it.
 *
 * @param type The edge class.
 * @param eventType The event that does not say where the identity is.
 * @param property The name of the identifying property of the edge.
 */
class UnknownGraphEventProperty(type: KClass<*>, eventType: KClass<*>, property: String) : IllegalArgumentException(
    "The mapping of '${type.displayName()}' refers to the property '$property', which " +
        "'${eventType.displayName()}' does not have."
)
