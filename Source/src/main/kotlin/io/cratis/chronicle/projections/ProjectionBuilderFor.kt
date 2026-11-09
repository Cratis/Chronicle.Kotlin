// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.util.function.Consumer
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

/**
 * Confirms [propertyName] is a real property of [type], failing clearly and immediately rather than
 * letting a typo reach the kernel as a broken projection definition.
 *
 * This is what backs every property-name-string overload on the declarative builder - the ones a Java
 * caller reaches for because it cannot produce a [KProperty1].
 *
 * @throws UnknownReadModelProperty when [propertyName] is not a property of [type].
 */
private fun requireProperty(type: KClass<*>, propertyName: String): String {
    if (type.memberProperties.none { it.name == propertyName }) {
        throw UnknownReadModelProperty(type, propertyName)
    }
    return propertyName
}

/**
 * Confirms [propertyName] is a `Map` property of [type], so a dictionary entry operation addresses a
 * property that can hold keyed entries.
 *
 * @throws UnknownReadModelProperty when [propertyName] is not a property of [type].
 * @throws IllegalArgumentException when the property is not a `Map`.
 */
private fun requireDictionaryProperty(type: KClass<*>, propertyName: String): String {
    val property = type.memberProperties.firstOrNull { it.name == propertyName }
        ?: throw UnknownReadModelProperty(type, propertyName)
    val classifier = property.returnType.classifier as? KClass<*>
    require(classifier != null && Map::class.java.isAssignableFrom(classifier.java)) {
        "'$propertyName' on '${type.simpleName}' is not a dictionary (Map) property, so it cannot hold keyed entries."
    }
    return propertyName
}

/** The target path of the dictionary entry of [propertyName] addressed by [key]. */
private fun dictionaryEntry(propertyName: String, key: DictionaryKey): String = "$propertyName.${key.segment}"

/** Wraps [expression] in the kernel's set once expression. */
private fun setOnceExpression(expression: String): String = "\$setOnce($expression)"

/** The kernel's expression for an identity property of whoever caused the event. */
private fun causedByExpression(property: CausedByProperty): String = "\$causedBy(${property.expressionName})"

class ProjectionBuilderFor<TReadModel : Any>(
    private val readModelClass: KClass<TReadModel>
) : IProjectionBuilderFor<TReadModel> {

    val fromEntries = mutableListOf<FromDefinitionEntry>()
    val joinEntries = mutableListOf<JoinDefinitionEntry>()
    val fromEveryProperties = mutableMapOf<String, String>()
    val removedWithEntries = mutableListOf<RemovedWithEntry>()
    val removedWithJoinEntries = mutableListOf<RemovedWithJoinEntry>()
    val childrenEntries = mutableListOf<ChildrenEntry>()

    /** The ordering declared for each children property, keyed by the property name. */
    val childrenOrderings = mutableMapOf<String, ChildrenOrdering>()
    val nestedEntries = mutableListOf<NestedEntry>()
    var isRewindable = true
        private set
    var autoMapEnabled = true
        private set
    var variantIdentity: KClass<*>? = null
        private set
    var variantKey: String = "EventSourceId"
        private set
    val enteringEventClasses = mutableListOf<KClass<*>>()

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
        builder.ordering?.let { childrenOrderings[property.name] = it }
        return this
    }

    override fun <TChild : Any> children(
        propertyName: String,
        childClass: Class<TChild>,
        configure: Consumer<IChildrenBuilderFor<TChild>>
    ): IProjectionBuilderFor<TReadModel> {
        val builder = ChildrenBuilderFor(childClass.kotlin)
        configure.accept(builder)
        val name = requireProperty(readModelClass, propertyName)
        childrenEntries.add(ChildrenEntry(name, builder.identifiedBy, builder.fromEntries))
        builder.ordering?.let { childrenOrderings[name] = it }
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

    override fun <TNested : Any> nested(
        propertyName: String,
        nestedClass: Class<TNested>,
        configure: Consumer<INestedBuilderFor<TNested>>
    ): IProjectionBuilderFor<TReadModel> {
        val builder = NestedBuilderFor(nestedClass.kotlin)
        configure.accept(builder)
        nestedEntries.add(NestedEntry(requireProperty(readModelClass, propertyName), builder.fromEntries, builder.clearWithEventClasses))
        return this
    }

    override fun <TIdentity : Any> variantOf(identity: KClass<TIdentity>, key: KProperty1<TReadModel, *>): IProjectionBuilderFor<TReadModel> {
        variantIdentity = identity
        variantKey = key.name
        return this
    }

    override fun <TIdentity : Any> variantOf(identity: KClass<TIdentity>, keyPropertyName: String): IProjectionBuilderFor<TReadModel> {
        variantIdentity = identity
        variantKey = requireProperty(readModelClass, keyPropertyName)
        return this
    }

    override fun <TEvent : Any> entersOn(eventClass: KClass<TEvent>): IProjectionBuilderFor<TReadModel> {
        enteringEventClasses.add(eventClass)
        // The entering event is what creates the variant, so it must have a From even when the
        // author maps no properties on it explicitly and leaves the mapping to AutoMap.
        if (fromEntries.none { it.eventClass == eventClass }) {
            fromEntries.add(FromDefinitionEntry(eventClass = eventClass, properties = emptyMap()))
        }
        return this
    }

    override fun notRewindable(): IProjectionBuilderFor<TReadModel> {
        isRewindable = false
        return this
    }

    override fun noAutoMap(): IProjectionBuilderFor<TReadModel> {
        autoMapEnabled = false
        return this
    }

    override fun autoMap(): IProjectionBuilderFor<TReadModel> {
        autoMapEnabled = true
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

    override fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this)

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

    override fun <TValue : Any?> count(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[property.name] = "\$count"
        return this
    }

    override fun count(propertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[requireProperty(readModelClass, propertyName)] = "\$count"
        return this
    }

    override fun <TValue : Any?> increment(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[property.name] = "\$increment"
        return this
    }

    override fun increment(propertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[requireProperty(readModelClass, propertyName)] = "\$increment"
        return this
    }

    override fun <TValue : Any?> decrement(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[property.name] = "\$decrement"
        return this
    }

    override fun decrement(propertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[requireProperty(readModelClass, propertyName)] = "\$decrement"
        return this
    }

    override fun <TValue : Any?> add(property: KProperty1<TReadModel, TValue>): IAddBuilderFor<TReadModel, TEvent> =
        AddBuilderFor(propertyMappings, property.name, this)

    override fun add(propertyName: String): IAddBuilderFor<TReadModel, TEvent> =
        AddBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this)

    override fun <TValue : Any?> subtract(property: KProperty1<TReadModel, TValue>): ISubtractBuilderFor<TReadModel, TEvent> =
        SubtractBuilderFor(propertyMappings, property.name, this)

    override fun subtract(propertyName: String): ISubtractBuilderFor<TReadModel, TEvent> =
        SubtractBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this)

    override fun <TValue : Any?> setOnce(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, this, once = true)

    override fun <TValue : Any?> setOnce(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this, once = true)

    override fun <TValue : Any?> set(
        property: KProperty1<TReadModel, Map<String, TValue>?>,
        key: DictionaryKey,
    ): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, dictionaryEntry(property.name, key), this)

    override fun <TValue : Any?> set(propertyName: String, key: DictionaryKey): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, dictionaryEntry(requireDictionaryProperty(readModelClass, propertyName), key), this)

    override fun <TValue : Any?> setOnce(
        property: KProperty1<TReadModel, Map<String, TValue>?>,
        key: DictionaryKey,
    ): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, dictionaryEntry(property.name, key), this, once = true)

    override fun <TValue : Any?> setOnce(propertyName: String, key: DictionaryKey): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(
            propertyMappings,
            dictionaryEntry(requireDictionaryProperty(readModelClass, propertyName), key),
            this,
            once = true,
        )

    override fun count(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(property.name, key, "\$count")

    override fun count(propertyName: String, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(requireDictionaryProperty(readModelClass, propertyName), key, "\$count")

    override fun increment(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(property.name, key, "\$increment")

    override fun increment(propertyName: String, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(requireDictionaryProperty(readModelClass, propertyName), key, "\$increment")

    override fun decrement(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(property.name, key, "\$decrement")

    override fun decrement(propertyName: String, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(requireDictionaryProperty(readModelClass, propertyName), key, "\$decrement")

    override fun add(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): IAddBuilderFor<TReadModel, TEvent> =
        AddBuilderFor(propertyMappings, dictionaryEntry(property.name, key), this)

    override fun add(propertyName: String, key: DictionaryKey): IAddBuilderFor<TReadModel, TEvent> =
        AddBuilderFor(propertyMappings, dictionaryEntry(requireDictionaryProperty(readModelClass, propertyName), key), this)

    override fun subtract(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): ISubtractBuilderFor<TReadModel, TEvent> =
        SubtractBuilderFor(propertyMappings, dictionaryEntry(property.name, key), this)

    override fun subtract(propertyName: String, key: DictionaryKey): ISubtractBuilderFor<TReadModel, TEvent> =
        SubtractBuilderFor(propertyMappings, dictionaryEntry(requireDictionaryProperty(readModelClass, propertyName), key), this)

    override fun removeKey(property: KProperty1<TReadModel, Map<String, *>?>, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(property.name, key, "\$remove")

    override fun removeKey(propertyName: String, key: DictionaryKey): IFromBuilderFor<TReadModel, TEvent> =
        entryOperation(requireDictionaryProperty(readModelClass, propertyName), key, "\$remove")

    private fun entryOperation(propertyName: String, key: DictionaryKey, expression: String): IFromBuilderFor<TReadModel, TEvent> {
        propertyMappings[dictionaryEntry(propertyName, key)] = expression
        return this
    }
}

class SetBuilderFor<TReadModel : Any, TEvent : Any, TValue : Any?> @JvmOverloads constructor(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromBuilderFor<TReadModel, TEvent>,
    private val once: Boolean = false,
) : ISetBuilderFor<TReadModel, TEvent, TValue> {

    @Deprecated("Ignores the lambda and maps the same-named event property; use toProperty or toValue instead")
    override fun to(expression: (TEvent) -> TValue?): IFromBuilderFor<TReadModel, TEvent> = assign(targetProperty)

    override fun toValue(value: TValue?): IFromBuilderFor<TReadModel, TEvent> = assign(valueExpression(value))

    override fun toEventSourceId(): IFromBuilderFor<TReadModel, TEvent> = assign("\$eventSourceId")

    override fun toProperty(eventProperty: String): IFromBuilderFor<TReadModel, TEvent> = assign(eventProperty)

    override fun toEventContextProperty(contextProperty: String): IFromBuilderFor<TReadModel, TEvent> =
        assign("\$eventContext($contextProperty)")

    override fun toCausedBy(property: CausedByProperty): IFromBuilderFor<TReadModel, TEvent> = assign(causedByExpression(property))

    private fun assign(expression: String): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = if (once) setOnceExpression(expression) else expression
        return parent
    }
}

/** Builds up an add operation - the event property whose value is added into the target property. */
class AddBuilderFor<TReadModel : Any, TEvent : Any>(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromBuilderFor<TReadModel, TEvent>
) : IAddBuilderFor<TReadModel, TEvent> {
    override fun with(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = "\$add($eventPropertyName)"
        return parent
    }
}

/** Builds up a subtract operation - the event property whose value is subtracted from the target property. */
class SubtractBuilderFor<TReadModel : Any, TEvent : Any>(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromBuilderFor<TReadModel, TEvent>
) : ISubtractBuilderFor<TReadModel, TEvent> {
    override fun with(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent> {
        mappings[targetProperty] = "\$subtract($eventPropertyName)"
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

    override fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), FromBuilderFor(readModelClass))

    override fun <TValue : Any?> setOnce(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, FromBuilderFor(readModelClass), once = true)

    override fun <TValue : Any?> setOnce(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), FromBuilderFor(readModelClass), once = true)

    override fun on(property: KProperty1<TReadModel, *>): IJoinBuilderFor<TReadModel, TEvent> {
        on = property.name
        return this
    }

    override fun on(propertyName: String): IJoinBuilderFor<TReadModel, TEvent> {
        on = requireProperty(readModelClass, propertyName)
        return this
    }
}

class FromEveryBuilderFor<TReadModel : Any>(
    private val readModelClass: KClass<TReadModel>
) : IFromEveryBuilderFor<TReadModel> {
    val propertyMappings = mutableMapOf<String, String>()

    override fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): IAllSetBuilderFor<TReadModel, TValue> =
        AllSetBuilderFor(propertyMappings, property.name, this)

    override fun <TValue : Any?> set(propertyName: String): IAllSetBuilderFor<TReadModel, TValue> =
        AllSetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this)

    override fun <TValue : Any?> setOnce(property: KProperty1<TReadModel, TValue>): IAllSetBuilderFor<TReadModel, TValue> =
        AllSetBuilderFor(propertyMappings, property.name, this, once = true)

    override fun <TValue : Any?> setOnce(propertyName: String): IAllSetBuilderFor<TReadModel, TValue> =
        AllSetBuilderFor(propertyMappings, requireProperty(readModelClass, propertyName), this, once = true)

    override fun <TValue : Any?> count(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${property.name}.\$eventContext.$keyFromContext"] = "\$count"
        return this
    }

    override fun count(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${requireProperty(readModelClass, propertyName)}.\$eventContext.$keyFromContext"] = "\$count"
        return this
    }

    override fun <TValue : Any?> increment(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${property.name}.\$eventContext.$keyFromContext"] = "\$increment"
        return this
    }

    override fun increment(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${requireProperty(readModelClass, propertyName)}.\$eventContext.$keyFromContext"] = "\$increment"
        return this
    }

    override fun <TValue : Any?> decrement(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${property.name}.\$eventContext.$keyFromContext"] = "\$decrement"
        return this
    }

    override fun decrement(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel> {
        propertyMappings["${requireProperty(readModelClass, propertyName)}.\$eventContext.$keyFromContext"] = "\$decrement"
        return this
    }
}

class AllSetBuilderFor<TReadModel : Any, TValue : Any?> @JvmOverloads constructor(
    private val mappings: MutableMap<String, String>,
    private val targetProperty: String,
    private val parent: IFromEveryBuilderFor<TReadModel>,
    private val once: Boolean = false,
) : IAllSetBuilderFor<TReadModel, TValue> {

    override fun toValue(value: TValue?): IFromEveryBuilderFor<TReadModel> = assign(valueExpression(value))

    override fun toProperty(eventProperty: String): IFromEveryBuilderFor<TReadModel> = assign(eventProperty)

    override fun toEventSourceId(): IFromEveryBuilderFor<TReadModel> = assign("\$eventSourceId")

    override fun toEventContextProperty(contextProperty: String): IFromEveryBuilderFor<TReadModel> =
        assign("\$eventContext($contextProperty)")

    override fun toCausedBy(property: CausedByProperty): IFromEveryBuilderFor<TReadModel> = assign(causedByExpression(property))

    private fun assign(expression: String): IFromEveryBuilderFor<TReadModel> {
        mappings[targetProperty] = if (once) setOnceExpression(expression) else expression
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
    var ordering: ChildrenOrdering? = null
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

    override fun prepend(): IChildrenBuilderFor<TChild> = order(ChildrenOrdering.prepend())

    override fun orderBy(property: KProperty1<TChild, *>): IChildrenBuilderFor<TChild> =
        order(ChildrenOrdering.sorted(property.name, SortDirection.ASCENDING))

    override fun orderBy(propertyName: String): IChildrenBuilderFor<TChild> =
        order(ChildrenOrdering.sorted(requireProperty(childClass, propertyName), SortDirection.ASCENDING))

    override fun orderByDescending(property: KProperty1<TChild, *>): IChildrenBuilderFor<TChild> =
        order(ChildrenOrdering.sorted(property.name, SortDirection.DESCENDING))

    override fun orderByDescending(propertyName: String): IChildrenBuilderFor<TChild> =
        order(ChildrenOrdering.sorted(requireProperty(childClass, propertyName), SortDirection.DESCENDING))

    private fun order(newOrdering: ChildrenOrdering): IChildrenBuilderFor<TChild> {
        if (ordering != null) throw ChildrenOrderingAlreadyDefined(childClass)
        ordering = newOrdering
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

    override fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TChild, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(childClass, propertyName), FromBuilderFor(childClass))

    override fun <TValue : Any?> setOnce(property: KProperty1<TChild, TValue>): ISetBuilderFor<TChild, TEvent, TValue> =
        SetBuilderFor(propertyMappings, property.name, FromBuilderFor(childClass), once = true)

    override fun <TValue : Any?> setOnce(propertyName: String): ISetBuilderFor<TChild, TEvent, TValue> =
        SetBuilderFor(propertyMappings, requireProperty(childClass, propertyName), FromBuilderFor(childClass), once = true)

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

/** Constant expressions use the invariant textual value consumed by the kernel. */
internal fun valueExpression(value: Any?): String {
    val actual = if (value is ConceptAs<*>) value.value() else value
    return when (actual) {
        null -> "\$null"
        is Enum<*> -> "\$value(${actual.ordinal})"
        is BigDecimal -> "\$value(${actual.toPlainString()})"
        else -> "\$value($actual)"
    }
}

class CompositeKeyBuilderFor : ICompositeKeyBuilderFor {
    private val parts = linkedMapOf<String, String>()

    override fun property(targetPropertyName: String, eventPropertyName: String): ICompositeKeyBuilderFor {
        parts[targetPropertyName] = eventPropertyName
        return this
    }

    override fun toEventContextProperty(targetPropertyName: String, contextProperty: String): ICompositeKeyBuilderFor {
        parts[targetPropertyName] = "\$eventContext($contextProperty)"
        return this
    }

    fun build(): String = "\$composite(" + parts.entries.joinToString(",") { "${it.key}=${it.value}" } + ")"
}
