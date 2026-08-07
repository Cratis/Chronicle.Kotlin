// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

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
        configure: ((IFromBuilderFor<TReadModel, TEvent>) -> Unit)?
    ): IProjectionBuilderFor<TReadModel> = from(eventClass.kotlin, configure)

    /** The same relying entirely on AutoMap, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> from(eventClass: Class<TEvent>): IProjectionBuilderFor<TReadModel> =
        from(eventClass.kotlin, null)

    /** Configures a join projection from [eventClass], correlated by event source id. */
    fun <TEvent : Any> join(
        eventClass: KClass<TEvent>,
        configure: ((IJoinBuilderFor<TReadModel, TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** Configures property mappings that apply to every event type the projection observes. Alias of [fromAll]. */
    fun fromEvery(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel>

    /** Configures property mappings that apply to every event type the projection observes. Alias of [fromEvery]. */
    fun fromAll(configure: (IFromEveryBuilderFor<TReadModel>) -> Unit): IProjectionBuilderFor<TReadModel>

    /** Specifies the event type that removes this read model instance. */
    fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: ((IKeyBuilderFor<TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** Specifies the joined event type that removes this read model instance. */
    fun <TEvent : Any> removedWithJoin(
        eventClass: KClass<TEvent>,
        configure: ((IRemovedWithJoinBuilderFor<TEvent>) -> Unit)? = null
    ): IProjectionBuilderFor<TReadModel>

    /** Configures a child collection projection on [property]. */
    fun <TChild : Any> children(
        property: KProperty1<TReadModel, *>,
        childClass: KClass<TChild>,
        configure: (IChildrenBuilderFor<TChild>) -> Unit
    ): IProjectionBuilderFor<TReadModel>

    /** Configures a nested single-object projection on [property]. */
    fun <TNested : Any> nested(
        property: KProperty1<TReadModel, *>,
        nestedClass: KClass<TNested>,
        configure: (INestedBuilderFor<TNested>) -> Unit
    ): IProjectionBuilderFor<TReadModel>

    /** Marks this projection as forward-only. */
    fun notRewindable(): IProjectionBuilderFor<TReadModel>
}

interface IFromBuilderFor<TReadModel : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: (TReadModel) -> KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>

    /** Uses the given event property as the key correlating events to read model instances, instead of the event source id. */
    fun usingKey(eventPropertyName: String): IFromBuilderFor<TReadModel, TEvent>

    /** Uses a fixed value as the key — every occurrence of [TEvent] updates the same read model instance. */
    fun usingConstantKey(value: String): IFromBuilderFor<TReadModel, TEvent>

    /** Uses a composite of multiple event properties as the key. */
    fun usingCompositeKey(configure: (ICompositeKeyBuilderFor) -> Unit): IFromBuilderFor<TReadModel, TEvent>
}

interface ISetBuilderFor<TReadModel : Any, TEvent : Any, TValue : Any?> {
    fun to(expression: (TEvent) -> TValue?): IFromBuilderFor<TReadModel, TEvent>
    fun toEventSourceId(): IFromBuilderFor<TReadModel, TEvent>
    fun toProperty(eventProperty: String): IFromBuilderFor<TReadModel, TEvent>
}

/** Builds up which event properties correlate to a read model property when joining against [TEvent]. */
interface IJoinBuilderFor<TReadModel : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>

    /** Sets the read model property that defines the relationship, i.e. what is being joined on. */
    fun on(property: KProperty1<TReadModel, *>): IJoinBuilderFor<TReadModel, TEvent>
}

/** Builds up property mappings that apply to every event type a projection observes. */
interface IFromEveryBuilderFor<TReadModel : Any> {
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): IAllSetBuilderFor<TReadModel, TValue>
}

interface IAllSetBuilderFor<TReadModel : Any, TValue : Any?> {
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

    /** Sets the property on [TChild] that is the child's own identity within the collection. */
    fun identifiedBy(propertyName: String): IChildrenBuilderFor<TChild>
}

interface IChildFromBuilderFor<TChild : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: KProperty1<TChild, TValue>): ISetBuilderFor<TChild, TEvent, TValue>
    fun usingKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent>
    fun usingParentKey(eventPropertyName: String): IChildFromBuilderFor<TChild, TEvent>
}

/** Builds up a nested single-object projection for a [TNested] type. */
interface INestedBuilderFor<TNested : Any> {
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IFromBuilderFor<TNested, TEvent>) -> Unit)? = null
    ): INestedBuilderFor<TNested>

    /** Defines what event clears (sets to null) the nested object. */
    fun <TEvent : Any> clearWith(eventClass: KClass<TEvent>): INestedBuilderFor<TNested>
}

/** Builds up a composite key expression from multiple event properties. */
interface ICompositeKeyBuilderFor {
    fun property(targetPropertyName: String, eventPropertyName: String): ICompositeKeyBuilderFor
}
