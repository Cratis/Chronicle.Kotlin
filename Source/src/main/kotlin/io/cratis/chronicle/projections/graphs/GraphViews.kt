// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Declares and checks the views over property graphs.
 *
 * A view is a [ViewOf] class. This reads the nodes, edges and views an application declares, checks every view
 * against the rules of graph views, and gives the [GraphViewDefinition] of each graph that has views.
 */
object GraphViews {
    /**
     * The definitions of the views declared by [declarations], one per graph that has views, sorted by graph name.
     *
     * Pass the node, edge and view classes - as Kotlin classes or as Java [Class] objects - and any
     * [IGraphProjection]. Anything that is not part of a graph is ignored.
     *
     * @throws InvalidGraphView When a view breaks a rule. Other views that are invalid are attached to it as
     *   suppressed exceptions.
     */
    @JvmStatic
    fun definitionsFor(vararg declarations: Any): List<GraphViewDefinition> {
        val normalized = declarations.map { if (it is Class<*>) it.kotlin else it }
        val result = GraphProjections("").declarationsFor(normalized)
        result.throwIfAnyViewIsInvalid()
        return result.registrations.map { it.views }.filter { it.views.isNotEmpty() }.sortedBy { it.graph }
    }
}
