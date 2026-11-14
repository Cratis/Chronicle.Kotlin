// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * Marks a member of a [ViewOf] view as a property of the edge that was followed to reach the view.
 *
 * It is only valid in a view that is the target of [Follows] relations of one edge type.
 *
 * @property source The name of the edge property the member reads. Defaults to the name of the member.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FromEdge(val source: String = "")
