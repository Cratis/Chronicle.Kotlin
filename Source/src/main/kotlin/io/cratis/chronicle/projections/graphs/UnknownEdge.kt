// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a relation follows a class that is not an edge declared in the graph of the view.
 *
 * The reason is `UnknownEdge`.
 */
class UnknownEdge(view: KClass<*>, member: String, edge: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The relation '$member' of the view '${view.displayName()}' follows '${edge.displayName()}', which is not an edge declared in the graph."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "UnknownEdge"
    }
}
