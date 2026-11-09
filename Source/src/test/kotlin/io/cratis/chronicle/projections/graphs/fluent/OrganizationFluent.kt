// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.fluent

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.GroupCreated
import io.cratis.chronicle.projections.graphs.IGraphProjection
import io.cratis.chronicle.projections.graphs.IGraphProjectionBuilder
import io.cratis.chronicle.projections.graphs.MemberAdded
import io.cratis.chronicle.projections.graphs.MemberRemoved
import io.cratis.chronicle.projections.graphs.PersonRegistered
import io.cratis.chronicle.projections.graphs.PersonRemoved
import io.cratis.chronicle.projections.graphs.PlaceVisited
import java.time.OffsetDateTime
import java.util.UUID

// The Organization graph declared with the fluent builder: the Kotlin counterpart of
// Source/Clients/DotNET.Specs/Projections/Graphs/for_GraphDefinitionBuilder/given/FluentOrganization.cs.
// The element types carry no projection annotations - the builder says everything about the events.

data class Person(val personId: UUID, val name: String)

data class Group(val groupId: UUID, val name: String, val memberCount: Int)

data class Place(val placeId: UUID)

@Edge("MEMBER_OF")
data class MemberOf(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Group::class) val groupId: UUID,
    val since: OffsetDateTime
)

@Edge("VISITED")
data class Visited(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Place::class) val placeId: UUID,
    @Key val visitId: UUID,
    val times: Int
)

@Graph("Organization")
class OrganizationGraph : IGraphProjection {
    override fun define(builder: IGraphProjectionBuilder) {
        builder
            .node(Person::class) { node ->
                node.identifiedBy(Person::personId)
                    .from(PersonRegistered::class)
                    .removedWith(PersonRemoved::class) { it.detach() }
            }
            .node(Group::class) { node ->
                node.identifiedBy(Group::groupId)
                    .from(GroupCreated::class)
                    .from(MemberAdded::class) { it.usingKey("groupId").increment(Group::memberCount) }
                    .from(MemberRemoved::class) { it.usingKey("groupId").decrement(Group::memberCount) }
            }
            .node(Place::class) { node ->
                node.identifiedBy(Place::placeId)
                    .from(PlaceVisited::class) { it.usingKey("placeId") }
            }
            .edge(MemberOf::class, Person::class, Group::class) { edge ->
                edge.from(MemberAdded::class) {
                    it.usingSourceKey("personId")
                        .usingTargetKey("groupId")
                        .set(MemberOf::since).toEventContextProperty("occurred")
                }.removedWith(MemberRemoved::class) { it.usingSourceKey("personId").usingTargetKey("groupId") }
            }
            .edge(Visited::class, Person::class, Place::class) { edge ->
                edge.from(PlaceVisited::class) {
                    it.usingSourceKey("personId")
                        .usingTargetKey("placeId")
                        .usingIdentity("visitId")
                        .count(Visited::times)
                }
            }
    }
}
