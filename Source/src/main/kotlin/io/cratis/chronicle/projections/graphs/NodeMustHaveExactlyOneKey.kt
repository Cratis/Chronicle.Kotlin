// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a node class does not have exactly one property marked [io.cratis.chronicle.keys.Key].
 *
 * The key is what identifies the node in the graph, so a node without one cannot be written, and a node
 * with two does not say which one identifies it.
 *
 * @param type The node class.
 * @param keys How many properties carry the key marker.
 */
class NodeMustHaveExactlyOneKey(type: KClass<*>, keys: Int) : IllegalArgumentException(
    "The node type '${type.displayName()}' must have exactly one property with the key annotation, but it has $keys."
)
