// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

import kotlin.reflect.KProperty1

/**
 * Represents an implementation of [IKeyBuilder].
 *
 * @param TEvent Type of event to build for.
 */
class KeyBuilder<TEvent : Any> : IKeyBuilder<TEvent> {
    private var resolved: ResolvedKey = ResolvedKey.EventSourceId

    /** The key resolved by whichever method was called last, or [ResolvedKey.EventSourceId] if none was. */
    fun build(): ResolvedKey = resolved

    override fun <TProperty : Any> usingKey(keyAccessor: KProperty1<TEvent, TProperty>) {
        resolved = ResolvedKey.Property(keyAccessor.name)
    }

    override fun usingKeyWithPropertyName(propertyName: String) {
        resolved = ResolvedKey.Property(propertyName)
    }

    override fun usingKeyFromContext(property: String) {
        resolved = ResolvedKey.Context(property)
    }

    override fun usingCompositeKey(builderCallback: (ICompositeKeyBuilder<TEvent>) -> Unit) {
        val builder = CompositeKeyBuilder<TEvent>()
        builderCallback(builder)
        resolved = ResolvedKey.Composite(builder.build())
    }
}

private class CompositeKeyBuilder<TEvent : Any> : ICompositeKeyBuilder<TEvent> {
    private val properties = mutableListOf<String>()

    fun build(): List<String> = properties.toList()

    override fun <TProperty : Any> add(propertyAccessor: KProperty1<TEvent, TProperty>): ICompositeKeyBuilder<TEvent> {
        properties.add(propertyAccessor.name)
        return this
    }

    override fun addWithPropertyName(propertyName: String): ICompositeKeyBuilder<TEvent> {
        properties.add(propertyName)
        return this
    }
}
