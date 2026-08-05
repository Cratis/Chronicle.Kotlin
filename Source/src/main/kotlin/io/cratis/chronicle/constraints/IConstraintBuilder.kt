// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface IConstraintBuilder {
    /**
     * Scope every constraint subsequently added through this builder per event source type -
     * uniqueness is checked separately within each event source type rather than globally.
     *
     * @return The builder for continuation.
     */
    fun perEventSourceType(): IConstraintBuilder

    /**
     * Scope every constraint subsequently added through this builder per event stream type -
     * uniqueness is checked separately within each event stream type rather than globally.
     *
     * @return The builder for continuation.
     */
    fun perEventStreamType(): IConstraintBuilder

    /**
     * Scope every constraint subsequently added through this builder per event stream id -
     * uniqueness is checked separately within each event stream id rather than globally.
     *
     * @return The builder for continuation.
     */
    fun perEventStreamId(): IConstraintBuilder

    fun <TEvent : Any> uniqueFor(eventClass: KClass<TEvent>, message: String = ""): IConstraintBuilder
    fun unique(configure: (IUniqueConstraintBuilder) -> Unit): IConstraintBuilder
}

interface IUniqueConstraintBuilder {
    /**
     * Specifies the event type and property the uniqueness constraint applies to.
     *
     * [property] must be an actual property reference (e.g. `SomeEvent::email`), not an arbitrary
     * lambda — a lambda cannot be reflected back to the property it reads, so passing one would
     * silently produce a constraint keyed on the wrong property.
     */
    fun <TEvent : Any, TValue : Any> on(eventClass: KClass<TEvent>, property: KProperty1<TEvent, TValue>): IUniqueConstraintBuilder

    /**
     * Java-friendly alternative to [on] — Java has no equivalent of a Kotlin property reference,
     * so Java callers (via [io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge]) specify
     * the property by name instead.
     */
    fun <TEvent : Any> onWithPropertyName(eventClass: KClass<TEvent>, propertyName: String): IUniqueConstraintBuilder
    fun ignoreCasing(): IUniqueConstraintBuilder
    fun withMessage(message: String): IUniqueConstraintBuilder
}
