// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when the view a relation leads to is anchored at a node label other than the one at the far end of the edge.
 *
 * The reason is `TargetLabelMismatch`.
 */
class TargetLabelMismatch(view: KClass<*>, member: String, target: KClass<*>, expected: String) : InvalidGraphView(
    REASON,
    view,
    "The relation '$member' of the view '${view.displayName()}' leads to '${target.displayName()}', which is not anchored at '$expected', the node at the far end of the edge."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "TargetLabelMismatch"
    }
}
