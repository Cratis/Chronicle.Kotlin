// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class FromDefinitionEntry(
    val eventClass: KClass<*>,
    val properties: Map<String, String>,
    val key: String = "EventSourceId"
)

class ProjectionBuilderFor<TReadModel : Any>(
    private val readModelClass: KClass<TReadModel>
) : IProjectionBuilderFor<TReadModel> {

    val fromEntries = mutableListOf<FromDefinitionEntry>()

    override fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IFromBuilderFor<TReadModel, TEvent>) -> Unit)?
    ): IProjectionBuilderFor<TReadModel> {
        val builder = FromBuilderFor<TReadModel, TEvent>(readModelClass)
        configure?.invoke(builder)
        fromEntries.add(
            FromDefinitionEntry(
                eventClass = eventClass,
                properties = builder.propertyMappings,
                key = "EventSourceId"
            )
        )
        return this
    }
}

class FromBuilderFor<TReadModel : Any, TEvent : Any>(
    private val readModelClass: KClass<TReadModel>
) : IFromBuilderFor<TReadModel, TEvent> {
    val propertyMappings = mutableMapOf<String, String>()

    override fun <TValue : Any?> set(property: (TReadModel) -> KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> {
        val prop = readModelClass.memberProperties.first()
        return SetBuilderFor(propertyMappings, prop.name, this)
    }

    override fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> {
        return SetBuilderFor(propertyMappings, property.name, this)
    }
}

class SetBuilderFor<TReadModel : Any, TEvent : Any, TValue : Any?>(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromBuilderFor<TReadModel, TEvent>
) : ISetBuilderFor<TReadModel, TEvent, TValue> {

    override fun to(expression: (TEvent) -> TValue?): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = targetProperty
        return parent
    }

    override fun toEventSourceId(): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = "\$eventSourceId"
        return parent
    }

    override fun toProperty(eventProperty: String): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = eventProperty
        return parent
    }
}
