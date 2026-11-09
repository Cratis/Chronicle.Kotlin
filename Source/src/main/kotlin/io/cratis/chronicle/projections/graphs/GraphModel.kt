// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/** What a graph consists of, read from annotations or from a fluent builder - the two produce the same model. */
internal data class GraphModel(val name: String, val nodes: List<GraphNodeModel>, val edges: List<GraphEdgeModel>)

/**
 * A node type and the events that project onto it. [keyProperty] is the property of the node holding its key.
 */
internal data class GraphNodeModel(
    val type: KClass<*>,
    val label: String,
    val keyProperty: String,
    val from: List<GraphNodeFromModel>,
    val removedWith: List<GraphNodeRemovedWithModel>
)

/**
 * An edge type and the events that project onto it. [excludedProperties] are the source and target
 * properties, which hold the keys of the end nodes and are not properties of the edge.
 */
internal data class GraphEdgeModel(
    val type: KClass<*>,
    val edgeType: String,
    val sourceNode: KClass<*>,
    val targetNode: KClass<*>,
    val identifyingProperty: String?,
    val excludedProperties: List<String>,
    val from: List<GraphEdgeFromModel>,
    val removedWith: List<GraphEdgeRemovedWithModel>
)

/** An event that creates or updates a node. A `null` key means the event source id. */
internal data class GraphNodeFromModel(
    val eventType: KClass<*>,
    val key: String?,
    val properties: Map<String, String>,
    val updateOnly: Boolean = false
)

/** An event that creates or updates an edge. A `null` key means the event source id. */
internal data class GraphEdgeFromModel(
    val eventType: KClass<*>,
    val sourceKey: String?,
    val targetKey: String?,
    val identity: String?,
    val properties: Map<String, String>
)

/** An event that deletes a node. A `null` key means the event source id. */
internal data class GraphNodeRemovedWithModel(val eventType: KClass<*>, val key: String?, val detach: Boolean)

/** An event that deletes an edge. A `null` key means the event source id. */
internal data class GraphEdgeRemovedWithModel(
    val eventType: KClass<*>,
    val sourceKey: String?,
    val targetKey: String?,
    val identity: String?
)
