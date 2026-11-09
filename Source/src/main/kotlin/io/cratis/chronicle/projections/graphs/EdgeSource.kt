// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Marks the property of an [Edge] that holds the key of the node the edge starts from.
 *
 * This is the .NET client's `[Source<TNode>]`. The property is not a property of the edge.
 *
 * @property value The node class the edge starts from. It is called `value` so that Java can write
 *   `@EdgeSource(Person.class)`.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EdgeSource(val value: KClass<*>)
