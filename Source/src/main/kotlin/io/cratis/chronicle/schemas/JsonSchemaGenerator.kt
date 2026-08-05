// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

private val gson = Gson()

/**
 * Generates JSON schemas from Kotlin and Java classes for artifacts registered with the Chronicle kernel.
 *
 * The kernel stores the schema alongside the artifact and uses it to describe the artifact's shape —
 * which properties exist and what type each one has. For read models it additionally infers the key
 * property from the schema. Property names are emitted exactly as they are serialized onto the wire,
 * so a schema always lines up with the JSON payload produced for the same class.
 */
internal object JsonSchemaGenerator {

    /**
     * Generates a JSON schema for the given class.
     *
     * @param cls Class to generate a schema for.
     * @return The schema as a JSON string.
     */
    fun generate(cls: KClass<*>): String = gson.toJson(schemaFor(cls))

    private fun schemaFor(cls: KClass<*>): Map<String, Any> =
        mapOf(
            "type" to "object",
            "properties" to cls.memberProperties.associate { it.name to schemaForType(it.returnType) }
        )

    private fun schemaForType(type: KType): Map<String, Any> {
        val classifier = type.classifier as? KClass<*> ?: return mapOf("type" to "string")
        return when {
            classifier == String::class -> mapOf("type" to "string")
            classifier == Int::class || classifier == Long::class ||
                classifier == Short::class || classifier == Byte::class -> mapOf("type" to "integer")
            classifier == Double::class || classifier == Float::class -> mapOf("type" to "number")
            classifier == Boolean::class -> mapOf("type" to "boolean")
            else -> mapOf("type" to "string")
        }
    }
}
