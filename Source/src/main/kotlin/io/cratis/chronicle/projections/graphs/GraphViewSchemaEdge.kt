// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * An edge of the graph the views of a [GraphViewDefinition] were validated against.
 *
 * @property type The type of the edge, such as `MEMBER_OF`.
 * @property source The label of the node the edge starts at.
 * @property target The label of the node the edge ends at.
 * @property identifiedBy The property identifying the edge among edges of one type between the same two nodes.
 * @property properties The properties of the edge, in declaration order, without the keys of its end nodes.
 */
data class GraphViewSchemaEdge(
    val type: String,
    val source: String,
    val target: String,
    val identifiedBy: String?,
    val properties: List<String>
)
