// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when an edge class has more than one property marked [io.cratis.chronicle.keys.Key].
 *
 * An edge is identified by its type, source and target. One key property can add an identity, so that
 * several edges of the same type can connect the same two nodes - a second one has no meaning.
 *
 * @param type The edge class.
 */
class EdgeCanHaveOnlyOneIdentifyingKey(type: KClass<*>) : IllegalArgumentException(
    "The edge type '${type.displayName()}' can have at most one property with the key annotation, which " +
        "identifies the edge among edges of the same type between the same two nodes."
)
