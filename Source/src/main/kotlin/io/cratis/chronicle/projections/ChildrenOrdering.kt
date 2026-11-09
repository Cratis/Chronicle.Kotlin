// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * How new children are placed in a children collection.
 *
 * @property placement Where a new child goes.
 * @property sortBy The property on the child to sort by - set only when [placement] is [ChildrenPlacement.SORTED].
 * @property direction The sort direction - only meaningful when [placement] is [ChildrenPlacement.SORTED].
 */
data class ChildrenOrdering(
    val placement: ChildrenPlacement,
    val sortBy: String = "",
    val direction: SortDirection = SortDirection.ASCENDING,
) {
    companion object {
        /** New children go to the start of the collection. */
        @JvmStatic
        fun prepend(): ChildrenOrdering = ChildrenOrdering(ChildrenPlacement.PREPEND)

        /** New children are kept sorted by [propertyName] in [direction]. */
        @JvmStatic
        fun sorted(propertyName: String, direction: SortDirection): ChildrenOrdering =
            ChildrenOrdering(ChildrenPlacement.SORTED, propertyName, direction)
    }
}
