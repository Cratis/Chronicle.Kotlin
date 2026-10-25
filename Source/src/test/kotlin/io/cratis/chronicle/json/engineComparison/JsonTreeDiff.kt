// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/** Lexical, order-aware differences; unlike JsonElement.equals, numbers retain their spelling. */
fun jsonTreeDiff(left: String, right: String): List<String> = buildList {
    compareJson(JsonParser.parseString(left), JsonParser.parseString(right), "$", this)
}.sorted()

private fun compareJson(left: JsonElement, right: JsonElement, path: String, differences: MutableList<String>) {
    when {
        kind(left) != kind(right) -> differences.add("$path: type $left -> $right")
        left.isJsonObject -> compareMembers(left, right, path, differences)
        left.isJsonArray -> {
            val a = left.asJsonArray
            val b = right.asJsonArray
            for (index in 0 until maxOf(a.size(), b.size())) {
                when {
                    index >= a.size() -> differences.add("$path[$index]: extra <absent> -> ${b[index]}")
                    index >= b.size() -> differences.add("$path[$index]: missing ${a[index]} -> <absent>")
                    else -> compareJson(a[index], b[index], "$path[$index]", differences)
                }
            }
        }
        left.toString() != right.toString() -> differences.add("$path: value $left -> $right")
    }
}

private fun compareMembers(left: JsonElement, right: JsonElement, path: String, differences: MutableList<String>) {
    val a = left.asJsonObject
    val b = right.asJsonObject
    // Missing/extra fields get their own entries; order compares only the common members.
    val commonLeft = a.keySet().filter { b.has(it) }
    val commonRight = b.keySet().filter { a.has(it) }
    if (commonLeft != commonRight) differences.add("$path: order $commonLeft -> $commonRight")
    for (key in a.keySet() + b.keySet()) {
        when {
            !a.has(key) -> differences.add("$path.$key: extra <absent> -> ${b[key]}")
            !b.has(key) -> differences.add("$path.$key: missing ${a[key]} -> <absent>")
            else -> compareJson(a[key], b[key], "$path.$key", differences)
        }
    }
}

private fun kind(value: JsonElement): String = when {
    value.isJsonObject -> "object"
    value.isJsonArray -> "array"
    value.isJsonNull -> "null"
    value.asJsonPrimitive.isString -> "string"
    value.asJsonPrimitive.isBoolean -> "boolean"
    else -> "number"
}
