// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a view with edge property members is the target of relations that follow more than one edge type.
 *
 * The reason is `EdgePropertyViewSharedAcrossEdges`.
 */
class EdgePropertyViewSharedAcrossEdges(view: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' has edge properties but is the target of relations that follow more than one edge type."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "EdgePropertyViewSharedAcrossEdges"
    }
}
