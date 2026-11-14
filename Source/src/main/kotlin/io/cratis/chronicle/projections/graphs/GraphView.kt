// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * A view over a property graph, as defined by a [ViewOf] class.
 *
 * @property name The name of the view, unique within its graph.
 * @property root The label of the node the view is anchored at.
 * @property members The members of the view, in declaration order.
 */
data class GraphView(val name: String, val root: String, val members: List<GraphViewMember>)
