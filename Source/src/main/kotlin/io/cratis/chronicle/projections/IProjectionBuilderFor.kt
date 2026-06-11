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
}

/**
 * Convenience overload that infers the event class from the reified type parameter so callers
 * can write `builder.from<EmployeeHired>()` instead of `builder.from(EmployeeHired::class)`.
 */
inline fun <TReadModel : Any, reified TEvent : Any> IProjectionBuilderFor<TReadModel>.from(
    noinline configure: ((IFromBuilderFor<TReadModel, TEvent>) -> Unit)? = null
): IProjectionBuilderFor<TReadModel> = from(TEvent::class, configure)

interface IFromBuilderFor<TReadModel : Any, TEvent : Any> {
    fun <TValue : Any?> set(property: (TReadModel) -> KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>
    fun <TValue : Any?> set(property: KProperty1<TReadModel, TValue>): ISetBuilderFor<TReadModel, TEvent, TValue>
}

interface ISetBuilderFor<TReadModel : Any, TEvent : Any, TValue : Any?> {
    fun to(expression: (TEvent) -> TValue?): IFromBuilderFor<TReadModel, TEvent>
    fun toEventSourceId(): IFromBuilderFor<TReadModel, TEvent>
    fun toProperty(eventProperty: String): IFromBuilderFor<TReadModel, TEvent>
}
