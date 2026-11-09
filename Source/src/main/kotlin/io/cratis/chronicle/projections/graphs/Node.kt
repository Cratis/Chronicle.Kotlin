// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Marks a class as a node of a property graph.
 *
 * A node has a label - the simple name of the class - a unique key and properties. Mark exactly one
 * property with [io.cratis.chronicle.keys.Key]. Project events onto the node with the model-bound
 * annotations a read model already uses: [io.cratis.chronicle.projections.FromEvent],
 * [io.cratis.chronicle.projections.RemovedWith] (with `detach`), [io.cratis.chronicle.projections.SetFrom],
 * [io.cratis.chronicle.projections.SetFromContext], [io.cratis.chronicle.projections.SetValue],
 * [io.cratis.chronicle.projections.Increment], [io.cratis.chronicle.projections.Decrement],
 * [io.cratis.chronicle.projections.Count], [io.cratis.chronicle.projections.AddFrom] and
 * [io.cratis.chronicle.projections.SubtractFrom].
 *
 * A property is a primitive or a collection of primitives. An object property is an error: model the
 * object as a node of its own and connect it with an [Edge]. A node cannot also be a
 * [io.cratis.chronicle.readModels.ReadModel].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Node
