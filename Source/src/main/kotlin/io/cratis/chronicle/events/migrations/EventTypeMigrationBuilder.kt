// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events.migrations

import com.google.gson.Gson
import kotlin.reflect.KProperty1

/**
 * Trailing-lambda style DSL builder for describing how properties of [TSource] map to properties
 * of [TTarget] during an event type migration, mirroring [io.cratis.chronicle.constraints.ConstraintBuilder]'s
 * fluent style.
 *
 * Each operation is recorded as a small JSON directive keyed by the target property name, matching
 * the wire representation the Chronicle kernel expects for `UpcastJmesPath`/`DowncastJmesPath`.
 *
 * @param TTarget The event type properties are being migrated to.
 * @param TSource The event type properties are being migrated from.
 */
class EventTypeMigrationBuilder<TTarget : Any, TSource : Any> {
    private val properties = linkedMapOf<String, Any?>()

    /**
     * Rename a property from [source] on the source generation to [target] on the target generation.
     *
     * @param target The property on [TTarget] to populate.
     * @param source The property on [TSource] to read the value from.
     * @return This builder, for chaining.
     */
    fun renamedFrom(target: KProperty1<TTarget, *>, source: KProperty1<TSource, *>): EventTypeMigrationBuilder<TTarget, TSource> {
        properties[target.name] = mapOf("\$rename" to source.name)
        return this
    }

    /**
     * Provide a default value for [target] when it did not exist in the source generation.
     *
     * @param target The property on [TTarget] to populate.
     * @param value The default value to use.
     * @return This builder, for chaining.
     */
    fun defaultValue(target: KProperty1<TTarget, *>, value: Any?): EventTypeMigrationBuilder<TTarget, TSource> {
        properties[target.name] = mapOf("\$defaultValue" to value)
        return this
    }

    /**
     * Split [source] into [target] by extracting the zero-based [part] after splitting on [separator].
     *
     * @param target The property on [TTarget] to populate.
     * @param source The property on [TSource] to split.
     * @param separator The separator to split on.
     * @param part The zero-based index of the part to extract.
     * @return This builder, for chaining.
     */
    fun split(
        target: KProperty1<TTarget, *>,
        source: KProperty1<TSource, *>,
        separator: String,
        part: Int
    ): EventTypeMigrationBuilder<TTarget, TSource> {
        properties[target.name] = mapOf(
            "\$split" to mapOf("source" to source.name, "separator" to separator, "part" to part)
        )
        return this
    }

    /**
     * Combine [sources] into [target] by concatenating their values with [separator].
     *
     * @param target The property on [TTarget] to populate.
     * @param separator The separator to join the source values with.
     * @param sources The properties on [TSource] to concatenate, in order.
     * @return This builder, for chaining.
     */
    fun combine(
        target: KProperty1<TTarget, *>,
        separator: String,
        vararg sources: KProperty1<TSource, *>
    ): EventTypeMigrationBuilder<TTarget, TSource> {
        properties[target.name] = mapOf(
            "\$combine" to mapOf("sources" to sources.map { it.name }, "separator" to separator)
        )
        return this
    }

    /**
     * Convert the recorded property operations to their JSON wire representation.
     *
     * @return The JSON representation, or `"{}"` when no operations were recorded.
     */
    fun toJson(): String = if (properties.isEmpty()) "{}" else Gson().toJson(properties)
}
