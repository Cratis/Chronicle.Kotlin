// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** What a member of a graph view is. */
enum class GraphViewMemberKind(
    /** The name of the kind in the definition JSON. */
    val value: String
) {
    /** A property of the node the view is anchored at. */
    Property("property"),

    /** A relation that follows one edge type to another view. */
    Relation("relation"),

    /** A property of the edge that was followed to reach the view. */
    EdgeProperty("edgeProperty"),
}
