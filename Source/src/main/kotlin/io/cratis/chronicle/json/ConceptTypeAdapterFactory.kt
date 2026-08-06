// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.concepts.ConceptAs
import java.lang.reflect.ParameterizedType

/**
 * Serializes a [ConceptAs] as the value it wraps, rather than as an object wrapping one.
 *
 * `BookId("dune")` goes on the wire as `"dune"`. That is what makes a concept something you can
 * introduce into an event already in production: the JSON does not change, the schema the kernel
 * validates against does not change, and every event stored before the concept existed still reads
 * back.
 *
 * Reading goes the other way, through the single-argument constructor every concept has - the
 * primary constructor of a `data class`, or the synthetic one of a `@JvmInline value class`.
 */
class ConceptTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!ConceptAs::class.java.isAssignableFrom(rawType)) return null

        val underlyingType = underlyingTypeOf(rawType) ?: return null
        // The generic argument is always the boxed type - `ConceptAs<Int>` erases to Integer - while
        // the constructor of a concept over a primitive takes the primitive. Comparing them boxed is
        // what makes a concept over an Int find its constructor at all.
        val constructor = rawType.declaredConstructors
            .firstOrNull { it.parameterCount == 1 && it.parameterTypes[0].boxed() == underlyingType.boxed() }
            ?: return null
        constructor.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val underlyingAdapter = gson.getAdapter(TypeToken.get(underlyingType)) as TypeAdapter<Any>

        @Suppress("UNCHECKED_CAST")
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) {
                    out.nullValue()
                    return
                }
                underlyingAdapter.write(out, (value as ConceptAs<*>).value)
            }

            override fun read(reader: JsonReader): T? {
                val underlying = underlyingAdapter.read(reader) ?: return null
                return constructor.newInstance(underlying) as T
            }
        }.nullSafe()
    }

    /** This type as its boxed equivalent, so a primitive and its wrapper compare equal. */
    private fun Class<*>.boxed(): Class<*> = when (this) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        Integer.TYPE -> Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        else -> this
    }

    /**
     * The type a concept wraps, read off the `ConceptAs<T>` it implements.
     *
     * Walks up the hierarchy so a concept declared through an intermediate interface of its own -
     * a shared `Identifier : ConceptAs<String>`, say - resolves just as a direct one does.
     */
    private fun underlyingTypeOf(rawType: Class<*>): Class<*>? {
        val queue = ArrayDeque<Class<*>>().apply { add(rawType) }
        val seen = mutableSetOf<Class<*>>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!seen.add(current)) continue

            for (candidate in current.genericInterfaces) {
                if (candidate is ParameterizedType && candidate.rawType == ConceptAs::class.java) {
                    return candidate.actualTypeArguments.firstOrNull() as? Class<*>
                }
            }

            current.interfaces.forEach { queue.add(it) }
            current.superclass?.let { queue.add(it) }
        }

        return null
    }
}
