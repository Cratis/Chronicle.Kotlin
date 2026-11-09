// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Marks a class as an edge of a property graph, connecting two nodes.
 *
 * Mark exactly one property with [EdgeSource] and one with [EdgeTarget]; they hold the keys of the end
 * nodes and are not properties of the edge. By default an edge is identified by its type, source and
 * target, so there is at most one edge of a type between two nodes. Mark one more property with
 * [io.cratis.chronicle.keys.Key] to identify the edge among edges of the same type between the same two
 * nodes - every [EdgeFrom] and [EdgeRemovedWith] of the edge then has to say which event property holds
 * the identity.
 *
 * @property value The type of the edge, such as `MEMBER_OF`. It is called `value` so that Java can write
 *   `@Edge("MEMBER_OF")`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Edge(val value: String)
