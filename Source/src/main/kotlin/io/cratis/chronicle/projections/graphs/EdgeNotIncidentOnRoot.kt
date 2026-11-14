// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a relation follows an edge that neither starts nor ends at the node label the view is anchored at, in the direction it asks for.
 *
 * The reason is `EdgeNotIncidentOnRoot`.
 */
class EdgeNotIncidentOnRoot(view: KClass<*>, member: String, edge: String) : InvalidGraphView(
    REASON,
    view,
    "The relation '$member' of the view '${view.displayName()}' follows the edge '$edge', which does not start or end at the node the view is anchored at."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "EdgeNotIncidentOnRoot"
    }
}
