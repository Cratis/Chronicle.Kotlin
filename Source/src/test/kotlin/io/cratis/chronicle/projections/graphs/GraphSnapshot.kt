// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Renders what a graph registers - the read model it is stored as and the projection that fills it - as
 * JSON with the wire names of the contract, so it can be compared with a definition written out by hand
 * or built by another client.
 */
internal object GraphSnapshot {
    fun of(registration: GraphRegistration): JsonObject {
        val readModel = registration.readModel
        val projection = registration.projection
        return JsonObject().apply {
            add("readModel", JsonObject().apply {
                add("type", JsonObject().apply {
                    addProperty("identifier", readModel.type.identifier)
                    addProperty("generation", readModel.type.generation)
                })
                addProperty("containerName", readModel.containerName)
                addProperty("displayName", readModel.displayName)
                addProperty("sinkTypeId", readModel.sink.typeId)
                addProperty("observerIdentifier", readModel.observerIdentifier)
                add("schema", JsonParser.parseString(readModel.schema))
            })
            add("projection", JsonObject().apply {
                addProperty("eventSequenceId", projection.eventSequenceId)
                addProperty("identifier", projection.identifier)
                addProperty("readModel", projection.readModel)
                addProperty("isActive", projection.isActive)
                addProperty("isRewindable", projection.isRewindable)
                addProperty("initialModelState", projection.initialModelState)
                add("graph", graph(projection.graph))
            })
        }
    }

    private fun graph(graph: ProjectionsOuterClass.GraphProjectionDefinition) = JsonObject().apply {
        add("nodes", JsonObject().apply { graph.nodesMap.forEach { (label, node) -> add(label, node(node)) } })
        add("edges", JsonObject().apply { graph.edgesMap.forEach { (type, edge) -> add(type, edge(edge)) } })
    }

    private fun node(node: ProjectionsOuterClass.GraphNodeProjectionDefinition) = JsonObject().apply {
        add("from", JsonObject().apply {
            node.fromMap.forEach { (event, from) ->
                add(event, JsonObject().apply {
                    addProperty("key", from.key)
                    add("properties", properties(from.propertiesMap))
                    addProperty("updateOnly", from.updateOnly)
                })
            }
        })
        add("removedWith", JsonObject().apply {
            node.removedWithMap.forEach { (event, removal) ->
                add(event, JsonObject().apply {
                    addProperty("key", removal.key)
                    addProperty("detach", removal.detach)
                })
            }
        })
    }

    private fun edge(edge: ProjectionsOuterClass.GraphEdgeProjectionDefinition) = JsonObject().apply {
        add("from", JsonObject().apply {
            edge.fromMap.forEach { (event, from) ->
                add(event, JsonObject().apply {
                    addProperty("sourceKey", from.sourceKey)
                    addProperty("targetKey", from.targetKey)
                    addProperty("identity", from.identity)
                    add("properties", properties(from.propertiesMap))
                })
            }
        })
        add("removedWith", JsonObject().apply {
            edge.removedWithMap.forEach { (event, removal) ->
                add(event, JsonObject().apply {
                    addProperty("sourceKey", removal.sourceKey)
                    addProperty("targetKey", removal.targetKey)
                    addProperty("identity", removal.identity)
                })
            }
        })
    }

    private fun properties(properties: Map<String, String>) = JsonObject().apply {
        properties.forEach { (name, expression) -> addProperty(name, expression) }
    }
}
