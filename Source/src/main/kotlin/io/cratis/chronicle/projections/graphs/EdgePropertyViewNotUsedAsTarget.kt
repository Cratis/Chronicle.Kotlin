// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a view with edge property members is not the target of any relation, so there is no edge to read the properties from.
 *
 * The reason is `EdgePropertyViewNotUsedAsTarget`.
 */
class EdgePropertyViewNotUsedAsTarget(view: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' has edge properties but is not the target of any relation."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "EdgePropertyViewNotUsedAsTarget"
    }
}
