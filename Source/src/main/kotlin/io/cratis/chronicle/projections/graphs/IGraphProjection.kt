// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * A property graph declared with a fluent builder instead of annotations.
 *
 * It produces exactly the definition the annotations would. Put [Graph] on the implementation to name the
 * graph; without it the graph is named after the package.
 *
 * ```kotlin
 * @Graph("Organization")
 * class OrganizationGraph : IGraphProjection {
 *     override fun define(builder: IGraphProjectionBuilder) {
 *         builder
 *             .node(Person::class) { it.identifiedBy(Person::personId).from(PersonRegistered::class) }
 *             .node(Group::class) { it.identifiedBy(Group::groupId).from(GroupCreated::class) }
 *             .edge(MemberOf::class, Person::class, Group::class) { edge ->
 *                 edge.from(MemberAdded::class) { it.usingSourceKey("personId").usingTargetKey("groupId") }
 *             }
 *     }
 * }
 * ```
 */
interface IGraphProjection {
    /**
     * Declares the nodes and edges of the graph and the events that project onto them.
     *
     * @param builder The builder to declare them on.
     */
    fun define(builder: IGraphProjectionBuilder)
}
