// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/** The direction a sorted children collection is kept in. */
enum class SortDirection {
    /** Smallest value first. */
    ASCENDING,

    /** Largest value first. */
    DESCENDING,
}
