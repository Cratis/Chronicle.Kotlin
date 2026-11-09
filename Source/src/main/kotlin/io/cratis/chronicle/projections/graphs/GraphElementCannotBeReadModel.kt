// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a node or an edge class is also annotated [io.cratis.chronicle.readModels.ReadModel].
 *
 * A graph element is stored by a graph sink as part of the graph; a read model is stored in a document or
 * relational sink as a whole. A type cannot be both.
 *
 * @param type The node or edge class.
 */
class GraphElementCannotBeReadModel(type: KClass<*>) : IllegalArgumentException(
    "The type '${type.displayName()}' is a node or an edge in a graph and cannot also be a read model."
)
