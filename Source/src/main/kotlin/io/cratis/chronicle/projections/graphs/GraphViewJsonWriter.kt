// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** Writes a [GraphViewDefinition] as its canonical JSON. */
internal object GraphViewJsonWriter {
    fun write(definition: GraphViewDefinition, includeSchema: Boolean): String {
        val text = StringBuilder()
        text.append("{\n")
        text.append("  \"graph\": ").append(quote(definition.graph)).append(",\n")
        definition.schema?.takeIf { includeSchema }?.let { text.append("  \"schema\": ").append(schema(it)).append(",\n") }
        if (definition.views.isEmpty()) {
            text.append("  \"views\": []\n")
        } else {
            text.append("  \"views\": [\n")
            val views = definition.views.sortedWith { left, right -> left.name.compareTo(right.name) }
            views.forEachIndexed { index, view ->
                text.append("    {\n")
                text.append("      \"name\": ").append(quote(view.name)).append(",\n")
                text.append("      \"root\": ").append(quote(view.root)).append(",\n")
                text.append("      \"members\": [\n")
                view.members.forEachIndexed { memberIndex, member ->
                    text.append("        ").append(member(member))
                    text.append(if (memberIndex < view.members.size - 1) ",\n" else "\n")
                }
                text.append("      ]\n")
                text.append(if (index < views.size - 1) "    },\n" else "    }\n")
            }
            text.append("  ]\n")
        }
        text.append("}\n")
        return text.toString()
    }

    private fun schema(schema: GraphViewSchema): String {
        val nodes = schema.nodes.map {
            "{ \"label\": ${quote(it.label)}, \"key\": ${quote(it.key)}, \"properties\": ${list(it.properties)} }"
        }
        val edges = schema.edges.map {
            val identity = it.identifiedBy?.let { identity -> ", \"identifiedBy\": ${quote(identity)}" } ?: ""
            "{ \"type\": ${quote(it.type)}, \"source\": ${quote(it.source)}, \"target\": ${quote(it.target)}" +
                "$identity, \"properties\": ${list(it.properties)} }"
        }
        return "{\n    \"nodes\": ${items(nodes)},\n    \"edges\": ${items(edges)}\n  }"
    }

    private fun items(items: List<String>): String =
        if (items.isEmpty()) "[]" else items.joinToString(",\n", "[\n", "\n    ]") { "      $it" }

    private fun list(values: List<String>): String =
        if (values.isEmpty()) "[]" else values.joinToString(", ", "[ ", " ]") { quote(it) }

    private fun member(member: GraphViewMember): String {
        val fields = mutableListOf("\"name\": ${quote(member.name)}", "\"kind\": ${quote(member.kind.value)}")
        member.source?.let { fields += "\"source\": ${quote(it)}" }
        member.edge?.let { fields += "\"edge\": ${quote(it)}" }
        member.direction?.let { fields += "\"direction\": ${quote(it.value)}" }
        member.cardinality?.let { fields += "\"cardinality\": ${quote(it.value)}" }
        member.view?.let { fields += "\"view\": ${quote(it)}" }
        return "{ ${fields.joinToString(", ")} }"
    }

    private fun quote(value: String): String {
        val text = StringBuilder("\"")
        for (character in value) {
            when {
                character == '"' -> text.append("\\\"")
                character == '\\' -> text.append("\\\\")
                character == '\n' -> text.append("\\n")
                character == '\r' -> text.append("\\r")
                character == '\t' -> text.append("\\t")
                character < ' ' -> text.append(String.format("\\u%04x", character.code))
                else -> text.append(character)
            }
        }
        return text.append('"').toString()
    }
}
