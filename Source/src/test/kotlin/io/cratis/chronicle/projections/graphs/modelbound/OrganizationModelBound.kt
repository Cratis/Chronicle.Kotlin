// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.modelbound

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeFrom
import io.cratis.chronicle.projections.graphs.EdgeRemovedWith
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.GroupCreated
import io.cratis.chronicle.projections.graphs.MemberAdded
import io.cratis.chronicle.projections.graphs.MemberRemoved
import io.cratis.chronicle.projections.graphs.Node
import io.cratis.chronicle.projections.graphs.PersonRegistered
import io.cratis.chronicle.projections.graphs.PersonRemoved
import io.cratis.chronicle.projections.graphs.PlaceVisited
import java.time.OffsetDateTime
import java.util.UUID

// The Organization graph declared with annotations: the Kotlin counterpart of the model-bound types in
// Source/Clients/DotNET.Specs/Projections/Graphs/for_GraphDefinitionBuilder/given/ModelBoundOrganization.cs.

@Graph("Organization")
@Node
@FromEvent(PersonRegistered::class)
@RemovedWith(PersonRemoved::class, detach = true)
data class Person(@Key val personId: UUID, val name: String)

@Graph("Organization")
@Node
@FromEvent(GroupCreated::class)
@FromEvent(MemberAdded::class, key = "groupId")
@FromEvent(MemberRemoved::class, key = "groupId")
data class Group(
    @Key val groupId: UUID,
    val name: String,
    @Increment(MemberAdded::class) @Decrement(MemberRemoved::class) val memberCount: Int
)

@Graph("Organization")
@Node
@FromEvent(PlaceVisited::class, key = "placeId")
data class Place(@Key val placeId: UUID)

@Graph("Organization")
@Edge("MEMBER_OF")
@EdgeFrom(MemberAdded::class, sourceKey = "personId", targetKey = "groupId")
@EdgeRemovedWith(MemberRemoved::class, sourceKey = "personId", targetKey = "groupId")
data class MemberOf(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Group::class) val groupId: UUID,
    @SetFromContext(MemberAdded::class, "occurred") val since: OffsetDateTime
)

@Graph("Organization")
@Edge("VISITED")
@EdgeFrom(PlaceVisited::class, sourceKey = "personId", targetKey = "placeId", identity = "visitId")
data class Visited(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Place::class) val placeId: UUID,
    @Key val visitId: UUID,
    @Count(PlaceVisited::class) val times: Int
)
