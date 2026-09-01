// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

import kotlin.reflect.KProperty1

/**
 * Builds the key a projection correlates an event to a read model instance by.
 *
 * Calling none of the methods below is fine - the key then defaults to the event source id, exactly
 * as it does today. This mirrors .NET's `Cratis.Chronicle.Keys.IKeyBuilder<TEvent>`.
 *
 * @param TEvent Type of event the builder is for.
 */
interface IKeyBuilder<TEvent : Any> {
    /**
     * Use a property on the event itself as the key.
     *
     * @param keyAccessor The property to use.
     */
    fun <TProperty : Any> usingKey(keyAccessor: KProperty1<TEvent, TProperty>)

    /**
     * Java-friendly alternative to [usingKey] - a Kotlin property reference has no Java equivalent,
     * so a Java caller supplies the property by name instead.
     *
     * @param propertyName Name of the property to use.
     */
    fun usingKeyWithPropertyName(propertyName: String)

    /**
     * Use a value from the event context - for example the event source id, or a correlation id -
     * rather than a property on the event payload, as the key.
     *
     * @param property Name of the [io.cratis.chronicle.events.EventContext] property to use.
     */
    fun usingKeyFromContext(property: String)

    /**
     * Use a composite of more than one property on the event as the key.
     *
     * @param builderCallback Callback for building the composite key.
     */
    fun usingCompositeKey(builderCallback: (ICompositeKeyBuilder<TEvent>) -> Unit)
}
