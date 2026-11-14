// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a view class is also a node or an edge. A view reads a graph; it is never part of it.
 *
 * The reason is `ViewIsAlsoSchema`.
 */
class ViewIsAlsoSchema(view: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' is also a node or an edge. A view type cannot be part of the graph it reads."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "ViewIsAlsoSchema"
    }
}
