// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a relation follows an edge that starts and ends at the same node label without saying in which direction.
 *
 * The reason is `AmbiguousDirection`.
 */
class AmbiguousDirection(view: KClass<*>, member: String, edge: String) : InvalidGraphView(
    REASON,
    view,
    "The relation '$member' of the view '${view.displayName()}' follows the edge '$edge', which starts and ends at the same node. Say whether it is followed outgoing or incoming."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "AmbiguousDirection"
    }
}
