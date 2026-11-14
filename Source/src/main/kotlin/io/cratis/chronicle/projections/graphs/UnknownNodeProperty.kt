// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a property member of a view names a property the node it is anchored at does not declare.
 *
 * The reason is `UnknownNodeProperty`.
 */
class UnknownNodeProperty(view: KClass<*>, member: String, root: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The member '$member' of the view '${view.displayName()}' is not a property of the node '${root.displayName()}'."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "UnknownNodeProperty"
    }
}
