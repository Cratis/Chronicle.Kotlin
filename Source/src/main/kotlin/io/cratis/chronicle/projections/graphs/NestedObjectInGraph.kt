// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a node or an edge has a property that is an object, or a collection of objects.
 *
 * A property of a graph element is a primitive or a collection of primitives. Model the object as a node
 * of its own and connect it with an edge instead.
 *
 * @param type The node or edge class.
 * @param property The offending property.
 */
class NestedObjectInGraph(type: KClass<*>, property: String) : IllegalArgumentException(
    "The property '$property' of '${type.displayName()}' is an object. " +
        "Nodes and edges can only have primitive properties and collections of primitives."
)
