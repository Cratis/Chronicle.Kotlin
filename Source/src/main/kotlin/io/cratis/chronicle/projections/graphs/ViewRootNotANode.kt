// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when the view is anchored at a class that is not a node declared in a graph.
 *
 * The reason is `ViewRootNotANode`.
 */
class ViewRootNotANode(view: KClass<*>, root: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' is anchored at '${root.displayName()}', which is not a node declared in a graph."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "ViewRootNotANode"
    }
}
