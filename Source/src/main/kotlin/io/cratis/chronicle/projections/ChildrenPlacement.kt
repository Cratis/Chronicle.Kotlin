// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/** Where a new child is placed in a children collection. */
enum class ChildrenPlacement {
    /** New children go to the end of the collection - the default. */
    APPEND,

    /** New children go to the start of the collection, so the newest comes first. */
    PREPEND,

    /** New children are inserted in the order of a property on the child. */
    SORTED,
}
