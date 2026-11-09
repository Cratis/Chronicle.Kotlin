// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import kotlin.reflect.KClass

/**
 * Writes the schema of a graph - the read model schema the kernel reads the `x-graph-*` annotations from to
 * learn what a node or an edge is, which property keys it and which nodes an edge connects.
 */
internal object GraphSchemaWriter {
    /** The schema of [model], as a JSON string. */
    fun write(model: GraphModel): String {
        val properties = JsonObject()
        for (node in model.nodes) {
            val element = element(node.type, emptyList())
            element.addProperty("x-graph-kind", "Node")
            element.addProperty("x-graph-key", node.keyProperty)
            properties.add(node.label, element)
        }

        for (edge in model.edges) {
            val element = element(edge.type, edge.excludedProperties)
            element.addProperty("x-graph-kind", "Edge")
            element.addProperty("x-graph-source", edge.sourceNode.simpleName ?: edge.sourceNode.java.simpleName)
            element.addProperty("x-graph-target", edge.targetNode.simpleName ?: edge.targetNode.java.simpleName)
            edge.identifyingProperty?.let { element.addProperty("x-graph-identity", it) }
            properties.add(edge.edgeType, element)
        }

        val schema = JsonObject()
        schema.addProperty("type", "object")
        schema.addProperty("title", model.name)
        schema.addProperty("x-graph", true)
        schema.add("properties", properties)
        return schema.toString()
    }

    /**
     * The schema of every property of [type] but [excluded], by name. A property of a node or an edge is a
     * primitive or a collection of primitives, so an object - or a collection of objects - is a
     * [NestedObjectInGraph].
     */
    fun propertiesOf(type: KClass<*>, excluded: Collection<String>): Map<String, JsonObject> {
        val result = linkedMapOf<String, JsonObject>()
        for ((name, property) in schemaPropertiesOf(type).entrySet()) {
            if (excluded.any { it.equals(name, ignoreCase = true) }) continue
            val schema = property.asJsonObject
            if (isObjectOrCollectionOfObjects(schema)) throw NestedObjectInGraph(type, name)
            result[name] = schema
        }
        return result
    }

    /** Whether the property [name] of [type] is an object, or a collection of objects. */
    fun isObjectProperty(type: KClass<*>, name: String): Boolean {
        val schema = schemaPropertiesOf(type).get(name)?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        return isObjectOrCollectionOfObjects(schema)
    }

    private fun element(type: KClass<*>, excluded: Collection<String>): JsonObject {
        val properties = JsonObject()
        for ((name, schema) in propertiesOf(type, excluded)) properties.add(name, schema.deepCopy())
        val element = JsonObject()
        element.addProperty("type", "object")
        element.add("properties", properties)
        return element
    }

    private fun schemaPropertiesOf(type: KClass<*>): JsonObject =
        JsonParser.parseString(JsonSchemaGenerator.generate(type)).asJsonObject.getAsJsonObject("properties") ?: JsonObject()

    private fun isObjectOrCollectionOfObjects(schema: JsonObject): Boolean =
        isObject(schema) || schema.get("items")?.takeIf { it.isJsonObject }?.asJsonObject?.let(::isObject) == true

    private fun isObject(schema: JsonObject): Boolean =
        schema.get("type")?.takeIf { it.isJsonPrimitive }?.asString == "object"
}
