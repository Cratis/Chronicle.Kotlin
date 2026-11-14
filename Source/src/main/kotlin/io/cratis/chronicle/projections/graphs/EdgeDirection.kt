// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** The direction in which a relation of a graph view traverses an edge. */
enum class EdgeDirection(
    /** The name of the direction in the definition JSON. */
    val value: String
) {
    /** From the source node of the edge to its target node. */
    Outgoing("outgoing"),

    /** From the target node of the edge back to its source node. */
    Incoming("incoming"),
}
