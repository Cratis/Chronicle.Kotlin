// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when the type of a relation member is not a view declared in the graph of the view.
 *
 * The reason is `RelationTargetNotAView`.
 */
class RelationTargetNotAView(view: KClass<*>, member: String, target: KClass<*>?) : InvalidGraphView(
    REASON,
    view,
    "The relation '$member' of the view '${view.displayName()}' leads to '${target?.displayName() ?: "an unknown type"}', which is not a view declared in the graph."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "RelationTargetNotAView"
    }
}
