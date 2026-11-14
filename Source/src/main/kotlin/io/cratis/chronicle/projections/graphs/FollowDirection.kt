// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** The direction in which a [Follows] relation traverses an [Edge], as declared. */
enum class FollowDirection {
    /** Derive the direction from the labels of the edge and the node the view is anchored at. */
    Derived,

    /** Follow the edge from its source node to its target node. */
    Outgoing,

    /** Follow the edge from its target node back to its source node. */
    Incoming,
}
