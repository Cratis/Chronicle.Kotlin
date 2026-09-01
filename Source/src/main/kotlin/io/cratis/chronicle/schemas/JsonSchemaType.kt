// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import kotlin.reflect.KClass

/**
 * Overrides the type a class is represented as in the generated JSON schema.
 *
 * A type that brings its own Gson `TypeAdapter` serializes to something other than its own shape -
 * a value object written as a single string, for instance. The generated schema is what Chronicle
 * stores and reads values against, so it has to describe what actually goes on the wire; without
 * this the schema would describe the Kotlin shape and the value would not round-trip. Adorn the
 * type with this annotation to state what its adapter actually produces.
 *
 * ```kotlin
 * @JsonSchemaType(String::class)
 * data class Money(val amount: Long, val currency: String)
 * ```
 *
 * @property type The type the adorned class is represented as in the JSON schema.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonSchemaType(val type: KClass<*>)
