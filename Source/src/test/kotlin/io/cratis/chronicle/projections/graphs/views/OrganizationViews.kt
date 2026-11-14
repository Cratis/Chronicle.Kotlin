// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.views

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.FollowDirection
import io.cratis.chronicle.projections.graphs.Follows
import io.cratis.chronicle.projections.graphs.FromEdge
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.Node
import io.cratis.chronicle.projections.graphs.ViewOf
import java.time.OffsetDateTime
import java.util.UUID

// The Organization graph of the shared graph-views conformance fixture, and its views, declared with annotations.

@Graph("Organization")
@Node
data class Person(@Key val personId: UUID, val name: String)

@Graph("Organization")
@Node
data class Group(@Key val groupId: UUID, val name: String)

@Graph("Organization")
@Edge("MEMBER_OF")
data class MemberOf(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Group::class) val groupId: UUID,
    val since: OffsetDateTime
)

@Graph("Organization")
@Edge("PART_OF")
data class PartOf(
    @EdgeSource(Group::class) val childId: UUID,
    @EdgeTarget(Group::class) val parentId: UUID
)

@Graph("Organization")
@Edge("HAS_ROLE")
data class HasRole(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Group::class) val groupId: UUID,
    @Key val assignmentId: UUID,
    val role: String,
    val since: OffsetDateTime
)

@ViewOf(Group::class)
data class GroupSummary(@Key val id: UUID, val name: String)

@ViewOf(Group::class)
data class GroupMembership(@Key val id: UUID, val name: String, @FromEdge val since: OffsetDateTime)

@ViewOf(Person::class)
data class MemberSummary(@Key val id: UUID, val name: String, @FromEdge val since: OffsetDateTime)

@ViewOf(Person::class)
data class PersonWithGroups(@Key val id: UUID, val name: String, @Follows(MemberOf::class) val groups: List<GroupMembership>)

@ViewOf(Group::class)
data class GroupWithMembers(@Key val id: UUID, val name: String, @Follows(MemberOf::class) val members: List<MemberSummary>)

@ViewOf(Group::class)
data class GroupWithParent(
    @Key val id: UUID,
    val name: String,
    @Follows(PartOf::class, FollowDirection.Outgoing) val parent: GroupSummary?
)

@ViewOf(Group::class)
data class RoleAssignment(@Key val id: UUID, @FromEdge val assignmentId: UUID, @FromEdge val role: String)

@ViewOf(Person::class)
data class PersonRoles(@Key val id: UUID, @Follows(HasRole::class) val roles: List<RoleAssignment>)

/** Every class of the Organization graph and its views, in the order the fixture lists the schema. */
val organizationViewDeclarations: List<Any> = listOf(
    Person::class, Group::class, MemberOf::class, PartOf::class, HasRole::class,
    GroupMembership::class, GroupSummary::class, GroupWithMembers::class, GroupWithParent::class,
    MemberSummary::class, PersonRoles::class, PersonWithGroups::class, RoleAssignment::class
)
