// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

import kotlin.reflect.KProperty1

/**
 * Builds a key composed of more than one property on an event, for the rare correlation that is
 * not fully described by a single value.
 *
 * @param TEvent Type of event the builder is for.
 */
interface ICompositeKeyBuilder<TEvent : Any> {
    /**
     * Add a property to the composite key, in the order the parts should be combined.
     *
     * @param propertyAccessor The property to include.
     * @return This builder, for chaining.
     */
    fun <TProperty : Any> add(propertyAccessor: KProperty1<TEvent, TProperty>): ICompositeKeyBuilder<TEvent>

    /**
     * Java-friendly alternative to [add] - a Kotlin property reference has no Java equivalent, so a
     * Java caller supplies the property by name instead.
     *
     * @param propertyName Name of the property to include.
     * @return This builder, for chaining.
     */
    fun addWithPropertyName(propertyName: String): ICompositeKeyBuilder<TEvent>
}
