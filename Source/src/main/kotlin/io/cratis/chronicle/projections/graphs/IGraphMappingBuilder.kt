// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KProperty1

/**
 * Maps the properties of a node or an edge from the event it is projected from. Node and edge properties
 * map the same way, and counters work on both.
 *
 * Every function exists twice: with a [KProperty1], and by property name for callers, such as Java, that
 * cannot produce one. A name that is not a property of [TElement] throws
 * [io.cratis.chronicle.projections.UnknownReadModelProperty], and an event property that the event does not
 * have throws [io.cratis.chronicle.projections.InvalidPropertyForType].
 *
 * Implemented by the client; applications should not implement this builder interface.
 */
interface IGraphMappingBuilder<TElement : Any, TSelf : Any> {
    /** Starts mapping [property] to a value of the event. */
    fun set(property: KProperty1<TElement, *>): IGraphSetBuilder<TSelf>

    /** The same, by property name. */
    fun set(propertyName: String): IGraphSetBuilder<TSelf>

    /** Adds the value of the event property [eventPropertyName] into [property]. */
    fun add(property: KProperty1<TElement, *>, eventPropertyName: String): TSelf

    /** The same, by property name. */
    fun add(propertyName: String, eventPropertyName: String): TSelf

    /** Subtracts the value of the event property [eventPropertyName] from [property]. */
    fun subtract(property: KProperty1<TElement, *>, eventPropertyName: String): TSelf

    /** The same, by property name. */
    fun subtract(propertyName: String, eventPropertyName: String): TSelf

    /** Bumps [property] up by one every time the event occurs. */
    fun increment(property: KProperty1<TElement, *>): TSelf

    /** The same, by property name. */
    fun increment(propertyName: String): TSelf

    /** Bumps [property] down by one every time the event occurs. */
    fun decrement(property: KProperty1<TElement, *>): TSelf

    /** The same, by property name. */
    fun decrement(propertyName: String): TSelf

    /** Turns [property] into an occurrence counter. */
    fun count(property: KProperty1<TElement, *>): TSelf

    /** The same, by property name. */
    fun count(propertyName: String): TSelf
}
