// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * The nodes and edges of a graph that its views were validated against.
 *
 * @property nodes The nodes of the graph.
 * @property edges The edges of the graph.
 */
data class GraphViewSchema(val nodes: List<GraphViewSchemaNode>, val edges: List<GraphViewSchemaEdge>)
