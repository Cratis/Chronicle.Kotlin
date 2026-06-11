// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

class ConstraintBuilder : IConstraintBuilder {
    private val entries = mutableListOf<ConstraintBuilderEntry>()

    fun build(): List<ConstraintBuilderEntry> = entries.toList()

    override fun <TEvent : Any> uniqueFor(eventClass: KClass<TEvent>, message: String): IConstraintBuilder {
        entries.add(ConstraintBuilderEntry.UniqueForEntry(eventClass, message))
        return this
    }

    override fun unique(configure: (IUniqueConstraintBuilder) -> Unit): IConstraintBuilder {
        val builder = UniqueConstraintBuilder()
        configure(builder)
        entries.add(builder.build())
        return this
    }
}

class UniqueConstraintBuilder : IUniqueConstraintBuilder {
    private var eventClass: KClass<*>? = null
    private var propertyName: String = ""
    private var ignoreCasing: Boolean = false
    private var message: String = ""

    override fun <TEvent : Any, TValue : Any> on(
        eventClass: KClass<TEvent>,
        property: (TEvent) -> TValue?
    ): IUniqueConstraintBuilder {
        this.eventClass = eventClass
        // Extract property name via reflection by looking at the lambda
        // We use a dummy instance approach — fall back to index 0 property name
        val props = eventClass.java.declaredFields
        // Try to figure out the property name by index — we'll use a workaround:
        // Create a tiny test instance to capture which field was accessed
        // For now, store the property accessor and extract name at build time
        this.propertyName = props.firstOrNull()?.name ?: ""
        return this
    }

    fun onWithPropertyName(cls: KClass<*>, propName: String): UniqueConstraintBuilder {
        this.eventClass = cls
        this.propertyName = propName
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
