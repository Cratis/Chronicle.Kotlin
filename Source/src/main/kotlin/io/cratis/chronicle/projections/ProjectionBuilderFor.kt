// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class FromDefinitionEntry(
    val eventClass: KClass<*>,
    val properties: Map<String, String>,
    val key: String = "EventSourceId",
    val parentKey: String? = null
)

data class JoinDefinitionEntry(
    val eventClass: KClass<*>,
    val on: String,
    val properties: Map<String, String>
)

data class RemovedWithEntry(
    val eventClass: KClass<*>,
    val key: String = "EventSourceId",
    val parentKey: String = "EventSourceId"
)

data class RemovedWithJoinEntry(
    val eventClass: KClass<*>,
    val key: String = "EventSourceId"
)

data class ChildrenEntry(
    val propertyName: String,
    val identifiedBy: String,
    val fromEntries: List<FromDefinitionEntry>
)

data class NestedEntry(
    val propertyName: String,
    val fromEntries: List<FromDefinitionEntry>,
    val clearWithEventClasses: List<KClass<*>>
)

class ProjectionBuilderFor<TReadModel : Any>(
    private val readModelClass: KClass<TReadModel>
) : IProjectionBuilderFor<TReadModel> {

    val fromEntries = mutableListOf<FromDefinitionEntry>()
    val joinEntries = mutableListOf<JoinDefinitionEntry>()
    val fromEveryProperties = mutableMapOf<String, String>()
    val removedWithEntries = mutableListOf<RemovedWithEntry>()
    val removedWithJoinEntries = mutableListOf<RemovedWithJoinEntry>()
    val childrenEntries = mutableListOf<ChildrenEntry>()
    val nestedEntries = mutableListOf<NestedEntry>()
    var isRewindable = true
        private set

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
                key = builder.key
            )
        )
        return this
    }

    override fun <TEvent : Any> join(
        eventClass: KClass<TEvent>,
        configure: ((IJoinBuilderFor<TReadModel, TEvent>) -> Unit)?
    ): IProjectionBuilderFor<TReadModel> {
        val builder = JoinBuilderFor<TReadModel, TEvent>(readModelClass)
        configure?.invoke(builder)
        joinEntries.add(JoinDefinitionEntry(eventClass, builder.on, builder.propertyMappings))
        return this
    }

    override fun fromEvery(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel> {
        val builder = FromEveryBuilderFor(readModelClass)
        configure(builder)
        fromEveryProperties.putAll(builder.propertyMappings)
        return this
    }

    override fun fromAll(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel> = fromEvery(configure)

    override fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: ((IKeyBuilderFor<TEvent>) -> Unit)?
    ): IProjectionBuilderFor<TReadModel> {
        val builder = KeyBuilderFor<TEvent>()
        configure?.invoke(builder)
        removedWithEntries.add(RemovedWithEntry(eventClass, builder.key, builder.parentKey))
        return this
    }

    override fun <TEvent : Any> removedWithJoin(
        eventClass: KClass<TEvent>,
        configure: ((IRemovedWithJoinBuilderFor<TEvent>) -> Unit)?
    ): IProjectionBuilderFor<TReadModel> {
        val builder = RemovedWithJoinBuilderFor<TEvent>()
        configure?.invoke(builder)
        removedWithJoinEntries.add(RemovedWithJoinEntry(eventClass, builder.key))
        return this
    }

    override fun <TChild : Any> children(
        property: KProperty1<TReadModel, *>,
        childClass: KClass<TChild>,
        configure: (IChildrenBuilderFor<TChild>) -> Unit
    ): IProjectionBuilderFor<TReadModel> {
        val builder = ChildrenBuilderFor(childClass)
        configure(builder)
        childrenEntries.add(ChildrenEntry(property.name, builder.identifiedBy, builder.fromEntries))
        return this
    }

    override fun <TNested : Any> nested(
        property: KProperty1<TReadModel, *>,
        nestedClass: KClass<TNested>,
        configure: (INestedBuilderFor<TNested>) -> Unit
    ): IProjectionBuilderFor<TReadModel> {
        val builder = NestedBuilderFor(nestedClass)
        configure(builder)
        nestedEntries.add(NestedEntry(property.name, builder.fromEntries, builder.clearWithEventClasses))
        return this
    }

    override fun notRewindable(): IProjectionBuilderFor<TReadModel> {
        isRewindable = false
        return this
    }
}

class FromBuilderFor<TReadModel : Any, TEvent : Any>(
    private val readModelClass: KClass<TReadModel>
) : IFromBuilderFor<TReadModel, TEvent> {
    val propertyMappings = mutableMapOf<String, String>()
    var key: String = "EventSourceId"
        private set

    override fun <TValue : Any?> set(property: (TReadModel) -> KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> {
        val prop = readModelClass.memberProperties.first()
        return SetBuilderFor(propertyMappings, prop.name, this)
    }

    override fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, this)

    override fun usingKey(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        key = eventPropertyName
        return this
    }

    override fun usingConstantKey(value: String): IFromBuilderFor<TReadModel, TEvent> {
        key = "\$value($value)"
        return this
    }

    override fun usingCompositeKey(configure: (ICompositeKeyBuilderFor) -> Unit): IFromBuilderFor<TReadModel, TEvent> {
        val builder = CompositeKeyBuilderFor()
        configure(builder)
        key = builder.build()
        return this
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

class JoinBuilderFor<TReadModel : Any, TEvent : Any>(
    private val readModelClass: KClass<TReadModel>
) : IJoinBuilderFor<TReadModel, TEvent> {
    val propertyMappings = mutableMapOf<String, String>()
    var on: String = ""
        private set

    override fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, FromBuilderFor(readModelClass))

    override fun on(property: KProperty1<TReadModel, *>): IJoinBuilderFor<TReadModel, TEvent> {
        on = property.name
        return this
    }
}

class FromEveryBuilderFor<TReadModel : Any>(
    private val readModelClass: KClass<TReadModel>
) : IFromEveryBuilderFor<TReadModel> {
    val propertyMappings = mutableMapOf<String, String>()

    override fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): IAllSetBuilderFor<TReadModel, TValue> =
        AllSetBuilderFor(propertyMappings, property.name, this)
}

class AllSetBuilderFor<TReadModel : Any, TValue : Any?>(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromEveryBuilderFor<TReadModel>
) : IAllSetBuilderFor<TReadModel, TValue> {

    override fun toProperty(eventProperty: String): IFromEveryBuilderFor<TReadModel> {
        mappings[targetProperty] = eventProperty
        return parent
    }

    override fun toEventSourceId(): IFromEveryBuilderFor<TReadModel> {
        mappings[targetProperty] = "\$eventSourceId"
        return parent
    }

    override fun toEventContextProperty(contextProperty: String): IFromEveryBuilderFor<TReadModel> {
        mappings[targetProperty] = "\$eventContext($contextProperty)"
        return parent
    }
}

class KeyBuilderFor<TEvent : Any> : IKeyBuilderFor<TEvent> {
    var key: String = "EventSourceId"
        private set
    var parentKey: String = "EventSourceId"
        private set

    override fun usingKey(eventPropertyName: String): IKeyBuilderFor<TEvent> {
        key = eventPropertyName
        return this
    }

    override fun usingParentKey(eventPropertyName: String): IKeyBuilderFor<TEvent> {
        parentKey = eventPropertyName
        return this
    }
}

class RemovedWithJoinBuilderFor<TEvent : Any> : IRemovedWithJoinBuilderFor<TEvent> {
    var key: String = "EventSourceId"
        private set

    override fun usingKey(eventPropertyName: String): IRemovedWithJoinBuilderFor<TEvent> {
        key = eventPropertyName
        return this
    }
}

class ChildrenBuilderFor<TChild : Any>(
    private val childClass: KClass<TChild>
) : IChildrenBuilderFor<TChild> {
    val fromEntries = mutableListOf<FromDefinitionEntry>()
    var identifiedBy: String = "EventSourceId"
        private set

    override fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: (IChildFromBuilderFor<TChild, TEvent>) -> Unit
    ): IChildrenBuilderFor<TChild> {
        val builder = ChildFromBuilderFor<TChild, TEvent>(childClass)
        configure(builder)
        fromEntries.add(FromDefinitionEntry(eventClass, builder.propertyMappings, builder.key, builder.parentKey))
        return this
    }

    override fun identifiedBy(propertyName: String): IChildrenBuilderFor<TChild> {
        identifiedBy = propertyName
        return this
    }
}

class ChildFromBuilderFor<TChild : Any, TEvent : Any>(
    private val childClass: KClass<TChild>
) : IChildFromBuilderFor<TChild, TEvent> {
    val propertyMappings = mutableMapOf<String, String>()
    var key: String = "EventSourceId"
        private set
    var parentKey: String = "EventSourceId"
        private set

    override fun <TValue : Any?> set(property: KProperty1<TChild, TValue>): ISetBuilderFor<TChild, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, FromBuilderFor(childClass))

    override fun usingKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent> {
        key = eventPropertyName
        return this
    }

    override fun usingParentKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent> {
        parentKey = eventPropertyName
        return this
    }
}

class NestedBuilderFor<TNested : Any>(
    private val nestedClass: KClass<TNested>
) : INestedBuilderFor<TNested> {
    val fromEntries = mutableListOf<FromDefinitionEntry>()
    val clearWithEventClasses = mutableListOf<KClass<*>>()

    override fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IFromBuilderFor<TNested, TEvent>) -> Unit)?
    ): INestedBuilderFor<TNested> {
        val builder = FromBuilderFor<TNested, TEvent>(nestedClass)
        configure?.invoke(builder)
        fromEntries.add(FromDefinitionEntry(eventClass, builder.propertyMappings, builder.key))
        return this
    }

    override fun <TEvent : Any> clearWith(eventClass: KClass<TEvent>): INestedBuilderFor<TNested> {
        clearWithEventClasses.add(eventClass)
        return this
    }
}

class CompositeKeyBuilderFor : ICompositeKeyBuilderFor {
    private val parts = linkedMapOf<String, String>()

    override fun property(targetPropertyName: String, eventPropertyName: String): ICompositeKeyBuilderFor {
        parts[targetPropertyName] = eventPropertyName
        return this
    }

    fun build(): String = "\$composite(" + parts.entries.joinToString(",") { "${it.key}=${it.value}" } + ")"
}
