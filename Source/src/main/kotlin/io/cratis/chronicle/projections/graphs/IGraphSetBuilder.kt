// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** Chooses what a node or an edge property is set to. Implemented by the client. */
interface IGraphSetBuilder<TSelf : Any> {
    /**
     * Sets the property to the event property [eventProperty].
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun toProperty(eventProperty: String): TSelf

    /** Sets the property to the event source id. */
    fun toEventSourceId(): TSelf

    /**
     * Sets the property to a property of the event context, such as `occurred`, rather than of the event.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event context has no such property.
     */
    fun toEventContextProperty(contextProperty: String): TSelf

    /** Sets the property to a constant, or clears it when [value] is `null`. */
    fun toValue(value: Any?): TSelf
}
