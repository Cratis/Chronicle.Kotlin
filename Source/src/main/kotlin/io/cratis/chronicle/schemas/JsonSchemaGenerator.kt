// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import io.cratis.chronicle.compliance.Pii
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

private val gson = Gson()
private val integerTypes = setOf(Int::class, Long::class, Short::class, Byte::class)
private val numberTypes = setOf(Double::class, Float::class)

/**
 * Generates JSON schemas for Kotlin classes using reflection over their member properties.
 *
 * This is the single source of truth for schema generation shared by event type registration
 * ([io.cratis.chronicle.events.EventTypesService]) and read model registration
 * ([io.cratis.chronicle.readModels.ReadModelsService]). Generating both through one path keeps
 * property typing consistent and ensures that [Pii]-annotated properties and types always carry
 * the same compliance metadata to the kernel, regardless of which registration path produced
 * the schema.
 */
object JsonSchemaGenerator {

    /**
     * Generates a JSON schema string describing [cls]'s member properties.
     *
     * @param cls The class to generate a schema for.
     * @return The generated JSON schema, as a JSON string.
     */
    fun generate(cls: KClass<*>): String = gson.toJson(generateNode(cls, mutableSetOf()))

    /** Builds the `{ "type": "object", "properties": {...} }` schema node for [cls]. */
    private fun generateNode(cls: KClass<*>, visiting: MutableSet<KClass<*>>): Map<String, Any> {
        // A read model or event can reference its own type recursively (e.g. a tree-shaped
        // structure). Without this guard, generating its schema would recurse forever.
        if (!visiting.add(cls)) {
            return mapOf("type" to "object")
        }
        try {
            val properties = cls.memberProperties.associate { prop -> prop.name to propertySchema(prop, visiting) }
            val schema = mutableMapOf<String, Any>("type" to "object", "properties" to properties)
            complianceMetadata(piiOf(cls))?.let { schema["compliance"] = it }
            return schema
        } finally {
            visiting.remove(cls)
        }
    }

    /** Builds the schema for a single member property, including its compliance metadata when [Pii]-marked. */
    private fun propertySchema(prop: KProperty1<*, *>, visiting: MutableSet<KClass<*>>): Map<String, Any> {
        val schema = schemaForType(prop.returnType, visiting)
        val typeClass = prop.returnType.classifier as? KClass<*>
        // A property can be marked [Pii] directly, or its declared type can be a value type that
        // is itself marked [Pii] (e.g. an `Email` wrapper reused across many events/read models) —
        // either is sufficient for the property to carry compliance metadata.
        val pii = piiOf(prop) ?: piiFieldFallback(prop) ?: piiOf(typeClass)
        complianceMetadata(pii)?.let { schema["compliance"] = it }
        return schema
    }

    /** Maps a Kotlin [KType] to its JSON schema representation. */
    private fun schemaForType(type: KType, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        val kClass = type.classifier as? KClass<*> ?: return mutableMapOf("type" to "string")
        val format = TypeFormats.formatFor(kClass)

        return when {
            kClass == String::class -> mutableMapOf("type" to "string")
            kClass in integerTypes -> mutableMapOf("type" to "integer")
            kClass in numberTypes -> mutableMapOf("type" to "number")
            kClass == Boolean::class -> mutableMapOf("type" to "boolean")
            kClass.java.isEnum -> enumSchema(kClass)
            kClass == Array::class || kClass.isSubclassOf(Collection::class) -> arraySchema(type, visiting)
            // A formatted type is a leaf, and its properties are deliberately left out. A geospatial
            // value serializes through its own GeoJSON adapter, so describing its Kotlin properties
            // would make the kernel flatten it into sub-properties and lose the coordinates. The
            // format alone tells the kernel what the value is.
            format != null -> mutableMapOf("type" to "object", "format" to format)
            isForeignType(kClass) -> mutableMapOf("type" to "string")
            else -> generateNode(kClass, visiting).toMutableMap()
        }
    }

    /** Builds a `"string"` schema carrying the enum's constant names, so the kernel can validate against them. */
    private fun enumSchema(kClass: KClass<*>): MutableMap<String, Any> {
        val names = kClass.java.enumConstants.map { (it as Enum<*>).name }
        return mutableMapOf("type" to "string", "enum" to names)
    }

    /** Builds an `"array"` schema whose `items` describe the collection's element type. */
    private fun arraySchema(type: KType, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        val elementType = type.arguments.firstOrNull()?.type
        val itemSchema: MutableMap<String, Any> =
            if (elementType != null) schemaForType(elementType, visiting) else mutableMapOf("type" to "object")
        val elementClass = elementType?.classifier as? KClass<*>
        complianceMetadata(piiOf(elementClass))?.let { itemSchema["compliance"] = it }
        return mutableMapOf("type" to "array", "items" to itemSchema)
    }

    /**
     * Determines whether [kClass] is a foundational Kotlin/Java type (dates, UUIDs, collections'
     * own supertypes, etc.) that should be represented as a plain `"string"` leaf rather than
     * reflected into as a nested object.
     */
    private fun isForeignType(kClass: KClass<*>): Boolean {
        val qualifiedName = kClass.qualifiedName ?: return true
        return qualifiedName.startsWith("kotlin.") || qualifiedName.startsWith("java.")
    }

    /** Reads the [Pii] annotation from a property or class, when present. */
    private fun piiOf(element: KAnnotatedElement?): Pii? = element?.findAnnotation<Pii>()

    /**
     * Falls back to reading [Pii] off the property's backing field.
     *
     * A `@Pii` on a primary constructor `val`/`var` is normally resolved onto the Kotlin property
     * itself, but this fallback keeps detection working for the `@field:Pii` use-site-target form too.
     */
    private fun piiFieldFallback(prop: KProperty1<*, *>): Pii? = prop.javaField?.getAnnotation(Pii::class.java)

    /** Converts a resolved [Pii] annotation into the `compliance` schema extension the kernel expects. */
    private fun complianceMetadata(pii: Pii?): List<Map<String, String>>? =
        pii?.let { listOf(mapOf("metadataType" to "PII", "details" to it.description)) }
}
