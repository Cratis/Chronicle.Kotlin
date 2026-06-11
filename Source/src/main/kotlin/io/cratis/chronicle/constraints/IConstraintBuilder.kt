// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

interface IConstraintBuilder {
    fun <TEvent : Any> uniqueFor(eventClass: KClass<TEvent>, message: String = ""): IConstraintBuilder
    fun unique(configure: (IUniqueConstraintBuilder) -> Unit): IConstraintBuilder
}

interface IUniqueConstraintBuilder {
    fun <TEvent : Any, TValue : Any> on(eventClass: KClass<TEvent>, property: (TEvent) -> TValue?): IUniqueConstraintBuilder
    fun ignoreCasing(): IUniqueConstraintBuilder
    fun withMessage(message: String): IUniqueConstraintBuilder
}
