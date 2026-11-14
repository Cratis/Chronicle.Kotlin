// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Marks a member of a [ViewOf] view as a relation: it follows one [Edge] from the node the view is anchored
 * at to another view.
 *
 * The type of the member gives the target view and the cardinality - `List<GroupMembership>` is many,
 * `GroupSummary?` is one, where an absent edge gives `null`.
 *
 * @property value The edge class that is followed.
 * @property direction The direction in which the edge is followed. It is derived from the labels of the
 *   edge for every edge but one that starts and ends at the same node label, which has to say.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Follows(val value: KClass<*>, val direction: FollowDirection = FollowDirection.Derived)
