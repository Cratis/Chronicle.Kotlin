// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Marks a class as a view over a property graph: a read model anchored at one [Node].
 *
 * The members of a view are the constructor properties of the class, in declaration order. Each is exactly
 * one of:
 *
 * - a property of the root node - an ordinary property, matched by name; the property marked
 *   [io.cratis.chronicle.keys.Key] is the key of the root node, whatever it is called in the view;
 * - a relation - a property marked [Follows], which follows one declared [Edge] to another view. A collection
 *   is a relation of cardinality many, a single (nullable) value a relation of cardinality one;
 * - an edge property - a property marked [FromEdge], a property of the edge that was followed to reach the
 *   view. It is only valid in a view that is the target of a relation.
 *
 * A view declares and reads; it is never projected onto. It cannot also be a [Node] or an [Edge]. The
 * declaration is checked, and turned into a [GraphViewDefinition], by [GraphViews].
 *
 * @property value The node class the view is anchored at. It is called `value` so that Java can write
 *   `@ViewOf(Person.class)`.
 * @property name The name of the view, unique within the graph. Defaults to the simple name of the class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewOf(val value: KClass<*>, val name: String = "")
