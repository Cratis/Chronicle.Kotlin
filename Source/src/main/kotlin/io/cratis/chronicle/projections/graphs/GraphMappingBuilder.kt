// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.projections.PropertyValidator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/** The mapping functions node and edge mappings share, collecting the expressions they produce. */
internal abstract class GraphMappingBuilder<TElement : Any, TSelf : Any>(
    private val elementClass: KClass<TElement>,
    protected val eventClass: KClass<*>
) : IGraphMappingBuilder<TElement, TSelf> {
    /** The property name to expression mappings collected so far. */
    val properties = linkedMapOf<String, String>()

    protected abstract val self: TSelf

    override fun set(property: KProperty1<TElement, *>): IGraphSetBuilder<TSelf> =
        GraphSetBuilder(self, eventClass, property.name, properties)

    override fun set(propertyName: String): IGraphSetBuilder<TSelf> =
        GraphSetBuilder(self, eventClass, requireProperty(elementClass, propertyName), properties)

    override fun add(property: KProperty1<TElement, *>, eventPropertyName: String): TSelf =
        arithmetic(property.name, "\$add", eventPropertyName)

    override fun add(propertyName: String, eventPropertyName: String): TSelf =
        arithmetic(requireProperty(elementClass, propertyName), "\$add", eventPropertyName)

    override fun subtract(property: KProperty1<TElement, *>, eventPropertyName: String): TSelf =
        arithmetic(property.name, "\$subtract", eventPropertyName)

    override fun subtract(propertyName: String, eventPropertyName: String): TSelf =
        arithmetic(requireProperty(elementClass, propertyName), "\$subtract", eventPropertyName)

    override fun increment(property: KProperty1<TElement, *>): TSelf = constant(property.name, "\$increment")

    override fun increment(propertyName: String): TSelf =
        constant(requireProperty(elementClass, propertyName), "\$increment")

    override fun decrement(property: KProperty1<TElement, *>): TSelf = constant(property.name, "\$decrement")

    override fun decrement(propertyName: String): TSelf =
        constant(requireProperty(elementClass, propertyName), "\$decrement")

    override fun count(property: KProperty1<TElement, *>): TSelf = constant(property.name, "\$count")

    override fun count(propertyName: String): TSelf = constant(requireProperty(elementClass, propertyName), "\$count")

    protected fun eventProperty(name: String): String = PropertyValidator.validatePropertyExists(eventClass, name)

    private fun constant(property: String, expression: String): TSelf {
        properties[property] = expression
        return self
    }

    private fun arithmetic(property: String, function: String, eventPropertyName: String): TSelf =
        constant(property, "$function(${eventProperty(eventPropertyName)})")
}
