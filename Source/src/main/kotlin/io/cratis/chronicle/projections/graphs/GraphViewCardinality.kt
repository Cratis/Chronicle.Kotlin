// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** How many nodes a relation of a graph view reaches. */
enum class GraphViewCardinality(
    /** The name of the cardinality in the definition JSON. */
    val value: String
) {
    /** A collection; no related node gives an empty list. */
    Many("many"),

    /** A single value; no related node gives `null`. */
    One("one"),
}
