// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a view contains itself, directly or through the views it leads to. Nesting is finite by construction.
 *
 * The reason is `RecursiveView`.
 */
class RecursiveView(view: KClass<*>) : InvalidGraphView(
    REASON,
    view,
    "The view '${view.displayName()}' contains itself, directly or through the views it leads to."
) {
    companion object {
        /** The stable name of the rule. */
        const val REASON = "RecursiveView"
    }
}
