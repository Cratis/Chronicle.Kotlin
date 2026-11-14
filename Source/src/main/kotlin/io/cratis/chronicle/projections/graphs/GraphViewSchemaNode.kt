// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * A node of the graph the views of a [GraphViewDefinition] were validated against.
 *
 * @property label The label of the node.
 * @property key The property holding the key of the node.
 * @property properties The properties of the node, in declaration order.
 */
data class GraphViewSchemaNode(val label: String, val key: String, val properties: List<String>)
