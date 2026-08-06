// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ConstraintBuilder : IConstraintBuilder {
    private val entries = mutableListOf<ConstraintBuilderEntry>()
    private var perEventSourceType = false
    private var perEventStreamType = false
    private var perEventStreamId = false

    fun build(): List<ConstraintBuilderEntry> = entries.toList()

    override fun perEventSourceType(): IConstraintBuilder {
        perEventSourceType = true
        return this
    }

    override fun perEventStreamType(): IConstraintBuilder {
        perEventStreamType = true
        return this
    }

    override fun perEventStreamId(): IConstraintBuilder {
        perEventStreamId = true
        return this
    }

    override fun <TEvent : Any> uniqueFor(eventClass: KClass<TEvent>, message: String): IConstraintBuilder {
        entries.add(ConstraintBuilderEntry.UniqueForEntry(eventClass, message, currentScope()))
        return this
    }

    override fun unique(configure: (IUniqueConstraintBuilder) -> Unit): IConstraintBuilder {
        val builder = UniqueConstraintBuilder()
        configure(builder)
        entries.add(builder.build().copy(scope = currentScope()))
        return this
    }

    /**
     * Captures the scope currently configured on this builder - `null` when none of
     * [perEventSourceType], [perEventStreamType], or [perEventStreamId] have been enabled, so
     * unscoped constraints keep working exactly as before this concept existed.
     */
    private fun currentScope(): ConstraintScope? =
        if (perEventSourceType || perEventStreamType || perEventStreamId) {
            ConstraintScope(perEventSourceType, perEventStreamType, perEventStreamId)
        } else {
            null
        }
}

class UniqueConstraintBuilder : IUniqueConstraintBuilder {
    private var eventClass: KClass<*>? = null
    private var propertyName: String = ""
    private var ignoreCasing: Boolean = false
    private var message: String = ""

    override fun <TEvent : Any, TValue : Any> on(
        eventClass: KClass<TEvent>,
        property: KProperty1<TEvent, TValue>
    ): IUniqueConstraintBuilder {
        this.eventClass = eventClass
        this.propertyName = property.name
        return this
    }

    override fun <TEvent : Any> onWithPropertyName(eventClass: KClass<TEvent>, propertyName: String): IUniqueConstraintBuilder {
        this.eventClass = eventClass
        this.propertyName = propertyName
        return this
    }

    override fun ignoreCasing(): IUniqueConstraintBuilder {
        this.ignoreCasing = true
        return this
    }

    override fun withMessage(msg: String): IUniqueConstraintBuilder {
        this.message = msg
        return this
    }

    fun build(): ConstraintBuilderEntry.UniqueEntry =
        ConstraintBuilderEntry.UniqueEntry(
            eventClass ?: error("on() must be called first"),
            propertyName,
            ignoreCasing,
            message
        )
}
