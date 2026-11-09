// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.projections.PropertyValidator
import io.cratis.chronicle.projections.valueExpression
import kotlin.reflect.KClass

/** Chooses what one property of a node or an edge is set to, writing the expression into [properties]. */
internal class GraphSetBuilder<TSelf : Any>(
    private val self: TSelf,
    private val eventClass: KClass<*>,
    private val property: String,
    private val properties: MutableMap<String, String>
) : IGraphSetBuilder<TSelf> {
    override fun toProperty(eventProperty: String): TSelf = expression(PropertyValidator.validatePropertyExists(eventClass, eventProperty))

    override fun toEventSourceId(): TSelf = expression("\$eventSourceId")

    override fun toEventContextProperty(contextProperty: String): TSelf =
        expression("\$eventContext(${PropertyValidator.validatePropertyExists(EventContext::class, contextProperty)})")

    override fun toValue(value: Any?): TSelf = expression(valueExpression(value))

    private fun expression(value: String): TSelf {
        properties[property] = value
        return self
    }
}
