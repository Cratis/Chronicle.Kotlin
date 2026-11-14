// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when two views of a graph have the same name.
 *
 * The reason is `DuplicateViewName`.
 */
class DuplicateViewName(view: KClass<*>, name: String, graph: String) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' is named '$name', as is another view of the graph '$graph'. View names are unique in a graph."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "DuplicateViewName"
    }
}
