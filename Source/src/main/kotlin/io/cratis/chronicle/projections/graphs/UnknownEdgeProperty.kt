// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when an edge property member of a view names a property the edge that is followed does not declare.
 *
 * The reason is `UnknownEdgeProperty`.
 */
class UnknownEdgeProperty(view: KClass<*>, member: String, edge: String) : InvalidGraphView(
    REASON,
    view,
    "The member '$member' of the view '${view.displayName()}' is not a property of the edge '$edge'."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "UnknownEdgeProperty"
    }
}
