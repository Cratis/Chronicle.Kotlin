// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface IProjectionBuilderFor<TReadModel : Any> {
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IFromBuilderFor<TReadModel, TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /**
     * The same, taking a Java [Class].
     *
     * Kotlin's [KClass] is awkward to produce from Java, and the default argument on the overload
     * above does not exist there either. This is what a Java projection calls.
     *
     * @param eventClass The event type to project from.
     * @param configure How to map its properties, or `null` to rely on AutoMap.
     * @return This builder, for chaining.
     */
    fun <TEvent : Any> from(
        eventClass: Class<TEvent>,
        configure: Consumer<IFromBuilderFor<TReadModel, TEvent>>
    ): IProjectionBuilderFor<TReadModel> = from(eventClass.kotlin) { configure.accept(it) }

    /** The same relying entirely on AutoMap, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> from(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> =
        from(eventClass.kotlin, null)

    /** Configures a join projection from [eventClass], correlated by event source id. */
    fun <TEvent : Any> join(
        eventClass: KClass<TEvent>,
        configure: ((IJoinBuilderFor<TReadModel, TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Class] - see [from]. */
    fun <TEvent : Any> join(
        eventClass: Class<TEvent>,
        configure: Consumer<IJoinBuilderFor<TReadModel, TEvent>>
    ): IProjectionBuilderFor<TReadModel> = join(eventClass.kotlin) { configure.accept(it) }

    /** The same with no configuration, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> join(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> =
        join(eventClass.kotlin, null)

    /** Configures property mappings that apply to every event type the projection observes. Alias of [fromAll]. */
    fun fromEvery(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel>

    /**
     * The same, taking a Java [Consumer] - see [from].
     *
     * A Kotlin function type is a `Function1` returning `Unit` from Java, which no Java lambda
     * writes willingly. Kotlin callers keep using the trailing-lambda overload above; this exists
     * so a Java projection can be written at all.
     */
    fun fromEvery(configure: Consumer<IFromEveryBuilderFor<TReadModel>>): IProjectionBuilderFor<TReadModel> =
        fromEvery { configure.accept(it) }

    /** Configures property mappings that apply to every event type the projection observes. Alias of [fromEvery]. */
    fun fromAll(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Consumer] - see [fromEvery]. */
    fun fromAll(configure: Consumer<IFromEveryBuilderFor<TReadModel>>): IProjectionBuilderFor<TReadModel> =
        fromAll { configure.accept(it) }

    /** Specifies the event type that removes this read model instance. */
    fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: ((IKeyBuilderFor<TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Class] - see [from]. */
    fun <TEvent : Any> removedWith(
        eventClass: Class<TEvent>,
        configure: Consumer<IKeyBuilderFor<TEvent>>
    ): IProjectionBuilderFor<TReadModel> = removedWith(eventClass.kotlin) { configure.accept(it) }

    /** The same with no configuration, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> removedWith(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> =
        removedWith(eventClass.kotlin, null)

    /** Specifies the joined event type that removes this read model instance. */
    fun <TEvent : Any> removedWithJoin(
        eventClass: KClass<TEvent>,
        configure: ((IRemovedWithJoinBuilderFor<TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Class] - see [from]. */
    fun <TEvent : Any> removedWithJoin(
        eventClass: Class<TEvent>,
        configure: Consumer<IRemovedWithJoinBuilderFor<TEvent>>
    ): IProjectionBuilderFor<TReadModel> = removedWithJoin(eventClass.kotlin) { configure.accept(it) }

    /** The same with no configuration, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> removedWithJoin(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> =
        removedWithJoin(eventClass.kotlin, null)

    /** Configures a child collection projection on [property]. */
    fun <TChild : Any> children(
        property: KProperty1<TReadModel, *>,
        childClass: KClass<TChild>,
        configure: (IChildrenBuilderFor<TChild>) -> Unit
    ): IProjectionBuilderFor<TReadModel>

    /**
     * The same, by property name and taking a Java [Class] - for callers, such as Java, that cannot
     * produce a [KProperty1] or [KClass].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun <TChild : Any> children(
        propertyName: String,
        childClass: Class<TChild>,
        configure: Consumer<IChildrenBuilderFor<TChild>>
    ): IProjectionBuilderFor<TReadModel>

    /** Configures a nested single-object projection on [property]. */
    fun <TNested : Any> nested(
        property: KProperty1<TReadModel, *>,
        nestedClass: KClass<TNested>,
        configure: (INestedBuilderFor<TNested>) -> Unit
    ): IProjectionBuilderFor<TReadModel>

    /**
     * The same, by property name and taking a Java [Class] - for callers, such as Java, that cannot
     * produce a [KProperty1] or [KClass].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun <TNested : Any> nested(
        propertyName: String,
        nestedClass: Class<TNested>,
        configure: Consumer<INestedBuilderFor<TNested>>
    ): IProjectionBuilderFor<TReadModel>

    /**
     * Declares this projection to be one of several mutually exclusive representations of the same
     * logical entity. Every projection declaring the same [identity] forms a group. Entering one
     * variant removes the entity from every other variant in that group. Only the events named with
     * [entersOn] may create this variant; every other event it projects from becomes an update-only
     * mapping that can bring an active instance up to date but never create or resurrect one.
     *
     * @param identity The type that anchors the logical identity shared by every variant in the group.
     * @param key The property on this read model that carries the shared identity.
     */
    fun <TIdentity : Any> variantOf(identity: KClass<TIdentity>, key: KProperty1<TReadModel, *>): IProjectionBuilderFor<TReadModel>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [keyPropertyName] is not a property of [TReadModel].
     */
    fun <TIdentity : Any> variantOf(identity: KClass<TIdentity>, keyPropertyName: String): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Class] - see [from]. */
    fun <TIdentity : Any> variantOf(identity: Class<TIdentity>, keyPropertyName: String): IProjectionBuilderFor<TReadModel> =
        variantOf(identity.kotlin, keyPropertyName)

    /**
     * Declares the event that activates this variant. Only for a projection that also declares
     * [variantOf]. The event keeps its ordinary create-or-update behavior; mapping its properties is
     * still done with the usual [from] call.
     *
     * @param eventClass Type of event that activates this variant.
     */
    fun <TEvent : Any> entersOn(eventClass: KClass<TEvent>): IProjectionBuilderFor<TReadModel>

    /** The same, taking a Java [Class] - see [from]. */
    fun <TEvent : Any> entersOn(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> = entersOn(eventClass.kotlin)

    /** Marks this projection as forward-only. */
    fun notRewindable(): IProjectionBuilderFor<TReadModel>

    /**
     * Disables AutoMap for this projection - only explicit mappings apply.
     *
     * @return This builder, for chaining.
     */
    fun noAutoMap(): IProjectionBuilderFor<TReadModel>

    /**
     * Re-enables AutoMap for this projection. AutoMap is on by default, so this is only needed to
     * turn it back on inside a scope where it was disabled with [noAutoMap].
     *
     * @return This builder, for chaining.
     */
    fun autoMap(): IProjectionBuilderFor<TReadModel>
}

interface IFromBuilderFor<TReadModel : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: (TReadModel) -> KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue>

    /** Uses the given event property as the key correlating events to read model instances, instead of the event source id. */
    fun usingKey(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent>

    /** Uses a fixed value as the key — every occurrence of [TEvent] updates the same read model instance. */
    fun usingConstantKey(value: String): IFromBuilderFor<TReadModel, TEvent>

    /** Uses a composite of multiple event properties as the key. */
    fun usingCompositeKey(configure: (ICompositeKeyBuilderFor) -> Unit): IFromBuilderFor<TReadModel, TEvent>

    /** The same, taking a Java [Consumer] - see [IProjectionBuilderFor.from]. */
    fun usingCompositeKey(configure: Consumer<ICompositeKeyBuilderFor>): IFromBuilderFor<TReadModel, TEvent> =
        usingCompositeKey { configure.accept(it) }

    /** Turns [property] into an occurrence counter — every [TEvent] bumps it by one. */
    fun <TValue : Any?> count(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun count(propertyName: String): IFromBuilderFor<TReadModel, TEvent>

    /** Bumps [property] up by one every time [TEvent] fires. */
    fun <TValue : Any?> increment(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun increment(propertyName: String): IFromBuilderFor<TReadModel, TEvent>

    /** Bumps [property] down by one every time [TEvent] fires. */
    fun <TValue : Any?> decrement(property: KProperty1<TReadModel, TValue>): IFromBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun decrement(propertyName: String): IFromBuilderFor<TReadModel, TEvent>

    /** Starts building an add operation, adding the value of an event property into [property]. */
    fun <TValue : Any?> add(property: KProperty1<TReadModel, TValue>): IAddBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun add(propertyName: String): IAddBuilderFor<TReadModel, TEvent>

    /** Starts building a subtract operation, subtracting the value of an event property from [property]. */
    fun <TValue : Any?> subtract(property: KProperty1<TReadModel, TValue>): ISubtractBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun subtract(propertyName: String): ISubtractBuilderFor<TReadModel, TEvent>
}

/** Implemented by the client; applications should not implement this builder interface. */
interface ISetBuilderFor<TReadModel : Any, TEvent : Any, TValue : Any?> {
    @Deprecated("Ignores the lambda and maps the same-named event property; use toProperty or toValue instead")
    fun to(expression: (TEvent) -> TValue?): IFromBuilderFor<TReadModel, TEvent>
    fun toValue(value: TValue?): IFromBuilderFor<TReadModel, TEvent>
    fun toEventSourceId(): IFromBuilderFor<TReadModel, TEvent>
    fun toProperty(eventProperty: String): IFromBuilderFor<TReadModel, TEvent>

    /** Maps to a property on the event context (e.g. the sequence number, or who caused it) rather than the event payload. */
    fun toEventContextProperty(contextProperty: String): IFromBuilderFor<TReadModel, TEvent>
}

/** Builds up an add operation — the property on the event whose value is added into the target property. */
interface IAddBuilderFor<TReadModel : Any, TEvent : Any> {
    fun with(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent>
}

/** Builds up a subtract operation — the property on the event whose value is subtracted from the target property. */
interface ISubtractBuilderFor<TReadModel : Any, TEvent : Any> {
    fun with(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent>
}

/** Builds up which event properties correlate to a read model property when joining against [TEvent]. */
interface IJoinBuilderFor<TReadModel : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TReadModel, TEvent, TValue>

    /** Sets the read model property that defines the relationship, i.e. what is being joined on. */
    fun on(property: KProperty1<TReadModel, *>): IJoinBuilderFor<TReadModel, TEvent>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun on(propertyName: String): IJoinBuilderFor<TReadModel, TEvent>
}

/** Builds up property mappings that apply to every event type a projection observes. */
interface IFromEveryBuilderFor<TReadModel : Any> {
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): IAllSetBuilderFor<TReadModel, TValue>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun <TValue : Any?> set(propertyName: String): IAllSetBuilderFor<TReadModel, TValue>

    /**
     * Counts every event into a dictionary-typed property, with the key resolved from [EventContext].
     * Each event increments the counter for the key produced by [keyFromContext].
     *
     * @param property The dictionary property (e.g., `Map<String, Long>`) to count into.
     * @param keyFromContext The [EventContext] property name that provides the dictionary key (e.g., "eventType.id").
     * @return This builder, for chaining.
     */
    fun <TValue : Any?> count(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun count(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel>

    /**
     * Increments a dictionary-typed property for each event, with the key resolved from [EventContext].
     * Each event adds 1 to the value for the key produced by [keyFromContext].
     *
     * @param property The dictionary property to increment.
     * @param keyFromContext The [EventContext] property name that provides the dictionary key.
     * @return This builder, for chaining.
     */
    fun <TValue : Any?> increment(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun increment(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel>

    /**
     * Decrements a dictionary-typed property for each event, with the key resolved from [EventContext].
     * Each event subtracts 1 from the value for the key produced by [keyFromContext].
     *
     * @param property The dictionary property to decrement.
     * @param keyFromContext The [EventContext] property name that provides the dictionary key.
     * @return This builder, for chaining.
     */
    fun <TValue : Any?> decrement(property: KProperty1<TReadModel, TValue>, keyFromContext: String): IFromEveryBuilderFor<TReadModel>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TReadModel].
     */
    fun decrement(propertyName: String, keyFromContext: String): IFromEveryBuilderFor<TReadModel>
}

/** Implemented by the client; applications should not implement this builder interface. */
interface IAllSetBuilderFor<TReadModel : Any, TValue : Any?> {
    fun toValue(value: TValue?): IFromEveryBuilderFor<TReadModel>
    fun toProperty(eventProperty: String): IFromEveryBuilderFor<TReadModel>
    fun toEventSourceId(): IFromEveryBuilderFor<TReadModel>
    fun toEventContextProperty(contextProperty: String): IFromEveryBuilderFor<TReadModel>
}

/** Builds up the key (and, where applicable, parent key) used to resolve which instance an event removes. */
interface IKeyBuilderFor<TEvent : Any> {
    fun usingKey(eventPropertyName: String): IKeyBuilderFor<TEvent>
    fun usingParentKey(eventPropertyName: String): IKeyBuilderFor<TEvent>
}

interface IRemovedWithJoinBuilderFor<TEvent : Any> {
    fun usingKey(eventPropertyName: String): IRemovedWithJoinBuilderFor<TEvent>
}

/** Builds up a child collection projection for a [TChild] element type. */
interface IChildrenBuilderFor<TChild : Any> {
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: (IChildFromBuilderFor<TChild, TEvent>) -> Unit
    ): IChildrenBuilderFor<TChild>

    /** The same, taking a Java [Class] - see [IProjectionBuilderFor.from]. */
    fun <TEvent : Any> from(
        eventClass: Class<TEvent>,
        configure: Consumer<IChildFromBuilderFor<TChild, TEvent>>
    ): IChildrenBuilderFor<TChild> = from(eventClass.kotlin) { configure.accept(it) }

    /** Sets the property on [TChild] that is the child's own identity within the collection. */
    fun identifiedBy(propertyName: String): IChildrenBuilderFor<TChild>
}

interface IChildFromBuilderFor<TChild : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: KProperty1<TChild, TValue>): ISetBuilderFor<TChild, TEvent, TValue>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws UnknownReadModelProperty when [propertyName] is not a property of [TChild].
     */
    fun <TValue : Any?> set(propertyName: String): ISetBuilderFor<TChild, TEvent, TValue>

    fun usingKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent>
    fun usingParentKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent>
}

/** Builds up a nested single-object projection for a [TNested] type. */
interface INestedBuilderFor<TNested : Any> {
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IFromBuilderFor<TNested, TEvent>) -> Unit)? = null
    ): INestedBuilderFor<TNested>

    /** The same, taking a Java [Class] - see [IProjectionBuilderFor.from]. */
    fun <TEvent : Any> from(
        eventClass: Class<TEvent>,
        configure: Consumer<IFromBuilderFor<TNested, TEvent>>
    ): INestedBuilderFor<TNested> = from(eventClass.kotlin) { configure.accept(it) }

    /** The same with no configuration, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> from(eventClass: Class<TEvent>): INestedBuilderFor<TNested> = from(eventClass.kotlin, null)

    /** Defines what event clears (sets to null) the nested object. */
    fun <TEvent : Any> clearWith(eventClass: KClass<TEvent>): INestedBuilderFor<TNested>

    /** The same, taking a Java [Class] - see [IProjectionBuilderFor.from]. */
    fun <TEvent : Any> clearWith(eventClass: Class<TEvent>): INestedBuilderFor<TNested> = clearWith(eventClass.kotlin)
}

/** Builds up a composite key expression from multiple event properties. */
interface ICompositeKeyBuilderFor {
    fun property(targetPropertyName: String, eventPropertyName: String): ICompositeKeyBuilderFor
}
