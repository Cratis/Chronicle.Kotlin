// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Names the property graph a node or an edge belongs to, or a fluent [IGraphProjection] builds.
 *
 * A graph is stored as one read model and filled by one projection. Put the same name on every node and
 * edge that belongs to it. Without this annotation the graph is named after the package of the type, which
 * is the JVM counterpart of the assembly the .NET client names a graph after - so types of one graph that
 * live in different packages need the annotation to end up in the same graph.
 *
 * @property value The name of the graph. It is the identifier of the read model the graph is registered as. It is
 *   called `value` so that Java can write `@Graph("Organization")`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Graph(val value: String)
