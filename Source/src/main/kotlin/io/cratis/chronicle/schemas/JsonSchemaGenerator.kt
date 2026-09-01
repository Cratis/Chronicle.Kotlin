// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.compliance.PiiNotSupportedOnEventSourceId
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.concepts.EventSourceId
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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
    private fun generateNode(cls: KClass<*>, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        // A read model or event can reference its own type recursively (e.g. a tree-shaped
        // structure). Without this guard, generating its schema would recurse forever.
        if (!visiting.add(cls)) {
            return mutableMapOf("type" to "object")
        }
        try {
            val properties = cls.memberProperties.associate { prop -> prop.name to propertySchema(prop, cls, visiting) }
            val schema = mutableMapOf<String, Any>("type" to "object", "properties" to properties)
            piiOf(cls)?.let { addComplianceMetadata(schema, complianceMetadata(it)) }
            return schema
        } finally {
            visiting.remove(cls)
        }
    }

    /** Builds the schema for a single member property, including its compliance metadata when [Pii]-marked. */
    private fun propertySchema(prop: KProperty1<*, *>, cls: KClass<*>, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        val schema = schemaForType(prop.returnType, visiting)
        val typeClass = prop.returnType.classifier as? KClass<*>
        // A property carries compliance metadata when any of these mark it PII, checked in order of
        // how specific the marker is to this exact property: directly on the property; on its
        // backing field (the `@field:Pii` use-site-target form); on the constructor parameter it was
        // built from (a Java record component, or a Kotlin property once `param` becomes the
        // annotation's resolved use-site target); on its declared type, when that type is itself a
        // value reused across many events/read models (e.g. an `Email` concept); or on the class that
        // declares this property, which marks every property that class has.
        val pii = piiOf(prop)
            ?: piiFieldFallback(prop)
            ?: piiConstructorParameterFallback(prop, cls)
            ?: piiOf(typeClass)
            ?: piiOf(cls)
        pii?.let { addComplianceMetadata(schema, complianceMetadata(it)) }
        return schema
    }

    /** Maps a Kotlin [KType] to its JSON schema representation. */
    private fun schemaForType(type: KType, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        val kClass = type.classifier as? KClass<*> ?: return mutableMapOf("type" to "string")
        val format = TypeFormats.formatFor(kClass)

        return when {
            // A concept serializes as the value it wraps, so the schema has to describe that value
            // rather than the wrapper. Describing the wrapper would have the kernel expect an object
            // where a plain string arrives, and would change the schema of every event that adopted
            // a concept for a property it already had.
            kClass.isSubclassOf(ConceptAs::class) -> conceptSchema(kClass, visiting)
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
            else -> generateNode(kClass, visiting)
        }
    }

    /**
     * Builds the schema for the value a concept wraps.
     *
     * The `value` property is what goes on the wire, so its type is what the schema describes. A
     * concept with no readable `value` falls back to a string, which is what every identifier
     * concept is anyway.
     */
    private fun conceptSchema(kClass: KClass<*>, visiting: MutableSet<KClass<*>>): MutableMap<String, Any> {
        val underlying = kClass.memberProperties.firstOrNull { it.name == "value" }?.returnType
            ?: return mutableMapOf("type" to "string")
        return schemaForType(underlying, visiting)
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
        piiOf(elementClass)?.let { addComplianceMetadata(itemSchema, complianceMetadata(it)) }
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

    /**
     * Adds [metadata] to [schema], descending into an object's `properties` so that the metadata
     * always lands on the leaves that actually hold a value.
     *
     * A compliance marker can be declared on something that is not a single value: a [Pii] on a
     * composite value-object type, or on a property whose type is such an object. Compliance is
     * applied per value, so leaving the marker on the container would make Chronicle store one
     * opaque ciphertext string where the schema still says `object`. Releasing that returns a
     * string, not an object, and the read model then fails to materialize. Pushing the metadata
     * down to every leaf keeps each value independently encrypted and preserves the document shape.
     *
     * An array-typed node is deliberately left as a container - coarse compliance on a whole
     * collection is handled separately by [arraySchema] adding metadata to the item schema, which
     * itself still descends into any object members the item schema has.
     */
    private fun addComplianceMetadata(schema: MutableMap<String, Any>, metadata: List<Map<String, String>>) {
        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as? Map<String, MutableMap<String, Any>>
        if (!properties.isNullOrEmpty()) {
            properties.values.forEach { addComplianceMetadata(it, metadata) }
            return
        }

        @Suppress("UNCHECKED_CAST")
        val compliance = schema.getOrPut("compliance") { mutableListOf<Map<String, String>>() } as MutableList<Map<String, String>>
        metadata.filter { entry -> !hasMetadataOfType(compliance, entry.getValue("metadataType")) }
            .forEach { compliance.add(it) }
    }

    /**
     * Whether [compliance] already carries an entry of [metadataType].
     *
     * A leaf can be reached by more than one marker - for example a [Pii] concept inside a value
     * object whose type is itself marked [Pii]. Recording the same metadata type twice adds nothing
     * and makes the generated schema noisier to read and to diff.
     */
    private fun hasMetadataOfType(compliance: List<Map<String, String>>, metadataType: String): Boolean =
        compliance.any { it["metadataType"] == metadataType }

    /** Reads the [Pii] annotation from a property or constructor parameter, when present. */
    private fun piiOf(element: KAnnotatedElement?): Pii? = element?.findAnnotation<Pii>()

    /**
     * Reads the [Pii] annotation from [cls], when present.
     *
     * Rejects [cls] when it is also an [EventSourceId]: the event source id is what the kernel uses
     * to look up the encryption key for every other PII value belonging to that source, so
     * encrypting the id itself would make its own key unfindable.
     */
    private fun piiOf(cls: KClass<*>?): Pii? {
        val pii = cls?.findAnnotation<Pii>() ?: return null
        if (cls.isSubclassOf(EventSourceId::class)) throw PiiNotSupportedOnEventSourceId(cls)
        return pii
    }

    /**
     * Falls back to reading [Pii] off the property's backing field.
     *
     * A `@Pii` on a primary constructor `val`/`var` is normally resolved onto the Kotlin property
     * itself, but this fallback keeps detection working for the `@field:Pii` use-site-target form,
     * and for a Java class whose field alone carries the annotation.
     */
    private fun piiFieldFallback(prop: KProperty1<*, *>): Pii? = prop.javaField?.getAnnotation(Pii::class.java)

    /**
     * Falls back to reading [Pii] off the constructor parameter [cls] built [prop] from.
     *
     * Once `@Pii` is applicable to a constructor parameter, Kotlin's default annotation
     * use-site-target resolution places it there in preference to the property or field - so a
     * plain `@Pii val email: String` on a primary constructor no longer resolves through [piiOf] or
     * [piiFieldFallback] and needs this fallback instead. It also covers a Java `record` component:
     * its canonical constructor parameter carries the annotation the same way a C# positional
     * record's constructor parameter does.
     */
    private fun piiConstructorParameterFallback(prop: KProperty1<*, *>, cls: KClass<*>): Pii? {
        cls.primaryConstructor?.parameters
            ?.firstOrNull { it.name.equals(prop.name, ignoreCase = true) }
            ?.let { piiOf(it) }
            ?.let { return it }

        val constructor = cls.java.declaredConstructors.maxByOrNull { it.parameterCount } ?: return null
        return constructor.parameters
            .firstOrNull { it.name.equals(prop.name, ignoreCase = true) }
            ?.getAnnotation(Pii::class.java)
    }

    /** Converts a resolved [Pii] annotation into the `compliance` schema extension the kernel expects. */
    private fun complianceMetadata(pii: Pii): List<Map<String, String>> =
        listOf(mapOf("metadataType" to "PII", "details" to pii.description))
}
