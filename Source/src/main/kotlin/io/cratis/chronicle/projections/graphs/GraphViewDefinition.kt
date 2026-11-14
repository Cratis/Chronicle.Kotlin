// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Every view declared over one property graph: the transport and conformance format of graph views.
 *
 * It is produced from declarations, never written by hand. [toJson] gives the canonical serialization every
 * Chronicle client emits for the same views.
 *
 * @property graph The name of the graph the views read.
 * @property views The views, sorted by name.
 * @property schema The nodes and edges the views were validated against. It is written by [toJson] only when asked.
 */
data class GraphViewDefinition(val graph: String, val views: List<GraphView>, val schema: GraphViewSchema? = null) {
    /**
     * The canonical JSON of the definition: UTF-8, LF line endings, two-space indentation, views sorted by
     * name in ordinal order, members in declaration order, and one trailing newline.
     *
     * @param includeSchema Also write the nodes and edges of the graph, between the graph name and the views,
     *   as the shared conformance fixture does. It has no effect on a definition without a [schema].
     */
    @JvmOverloads
    fun toJson(includeSchema: Boolean = false): String = GraphViewJsonWriter.write(this, includeSchema)
}
