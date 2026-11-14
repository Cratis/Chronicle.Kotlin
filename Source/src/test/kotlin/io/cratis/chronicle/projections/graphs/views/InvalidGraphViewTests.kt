// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.views

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.graphs.AmbiguousDirection
import io.cratis.chronicle.projections.graphs.DuplicateViewName
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeNotIncidentOnRoot
import io.cratis.chronicle.projections.graphs.EdgePropertyViewNotUsedAsTarget
import io.cratis.chronicle.projections.graphs.EdgePropertyViewSharedAcrossEdges
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.FollowDirection
import io.cratis.chronicle.projections.graphs.Follows
import io.cratis.chronicle.projections.graphs.FromEdge
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.GraphViews
import io.cratis.chronicle.projections.graphs.InvalidGraphView
import io.cratis.chronicle.projections.graphs.Node
import io.cratis.chronicle.projections.graphs.RecursiveView
import io.cratis.chronicle.projections.graphs.RelationTargetNotAView
import io.cratis.chronicle.projections.graphs.TargetLabelMismatch
import io.cratis.chronicle.projections.graphs.UnknownEdge
import io.cratis.chronicle.projections.graphs.UnknownEdgeProperty
import io.cratis.chronicle.projections.graphs.UnknownNodeProperty
import io.cratis.chronicle.projections.graphs.ViewIsAlsoSchema
import io.cratis.chronicle.projections.graphs.ViewOf
import io.cratis.chronicle.projections.graphs.ViewRootNotANode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.reflect.KClass

private class NotANode

private class NotAnEdge

@Graph("Organization")
@Node
private data class Place(@Key val placeId: UUID)

@Graph("Organization")
@Edge("VISITED")
private data class Visited(@EdgeSource(Person::class) val personId: UUID, @EdgeTarget(Place::class) val placeId: UUID)

@ViewOf(NotANode::class)
private data class ViewOfSomethingThatIsNotANode(val name: String)

@ViewOf(Person::class)
@Node
@Graph("Organization")
private data class ViewThatIsANode(@Key val id: UUID)

@ViewOf(Group::class, name = "GroupSummary")
private data class AnotherGroupSummary(@Key val id: UUID)

@ViewOf(Person::class)
private data class PersonWithNickname(@Key val id: UUID, val nickname: String)

@ViewOf(Person::class)
private data class PersonFollowingNothing(@Key val id: UUID, @Follows(NotAnEdge::class) val things: List<GroupSummary>)

@ViewOf(Group::class)
private data class GroupWithUnknownEdgeProperty(@Key val id: UUID, @FromEdge val until: OffsetDateTime)

@ViewOf(Person::class)
private data class PersonWithUnknownEdgeProperty(@Key val id: UUID, @Follows(MemberOf::class) val groups: List<GroupWithUnknownEdgeProperty>)

@ViewOf(Group::class)
private data class GroupVisiting(@Key val id: UUID, @Follows(Visited::class) val places: List<GroupSummary>)

@ViewOf(Group::class)
private data class GroupWithAmbiguousParent(@Key val id: UUID, @Follows(PartOf::class) val parent: GroupSummary?)

@ViewOf(Group::class)
private data class GroupWithContradictingDirection(
    @Key val id: UUID,
    @Follows(MemberOf::class, FollowDirection.Outgoing) val members: List<MemberSummary>
)

@ViewOf(Person::class)
private data class PersonWithGroupsAsPeople(@Key val id: UUID, @Follows(MemberOf::class) val groups: List<MemberSummary>)

@ViewOf(Person::class)
private data class PersonWithGroupNodes(@Key val id: UUID, @Follows(MemberOf::class) val groups: List<Group>)

@ViewOf(Group::class)
private data class GroupTree(
    @Key val id: UUID,
    @Follows(PartOf::class, FollowDirection.Outgoing) val parent: GroupTree?
)

@ViewOf(Group::class)
private data class OrphanEdgeView(@Key val id: UUID, @FromEdge val since: OffsetDateTime)

@ViewOf(Group::class)
private data class SharedEdgeView(@Key val id: UUID, @FromEdge val since: OffsetDateTime)

@ViewOf(Person::class)
private data class PersonWithTwoEdges(
    @Key val id: UUID,
    @Follows(MemberOf::class) val memberships: List<SharedEdgeView>,
    @Follows(HasRole::class) val roles: List<SharedEdgeView>
)

class InvalidGraphViewTests {
    private val graph: List<KClass<*>> = listOf(Person::class, Group::class, MemberOf::class, PartOf::class, HasRole::class)

    private inline fun <reified TFailure : InvalidGraphView> assertRejected(reason: String, vararg declarations: KClass<*>): TFailure {
        val failure = assertThrows(InvalidGraphView::class.java) { GraphViews.definitionsFor(*graph.toTypedArray(), *declarations) }
        assertInstanceOf(TFailure::class.java, failure)
        assertEquals(reason, failure.reason)
        return failure as TFailure
    }

    @Test
    fun `rejects a view anchored at something that is not a node`() {
        assertRejected<ViewRootNotANode>("ViewRootNotANode", ViewOfSomethingThatIsNotANode::class)
    }

    @Test
    fun `rejects a view that is also a node`() {
        assertRejected<ViewIsAlsoSchema>("ViewIsAlsoSchema", ViewThatIsANode::class)
    }

    @Test
    fun `rejects two views of a graph with the same name`() {
        assertRejected<DuplicateViewName>("DuplicateViewName", GroupSummary::class, AnotherGroupSummary::class)
    }

    @Test
    fun `rejects a property the node does not have`() {
        assertRejected<UnknownNodeProperty>("UnknownNodeProperty", PersonWithNickname::class)
    }

    @Test
    fun `rejects a relation that follows something that is not an edge of the graph`() {
        assertRejected<UnknownEdge>("UnknownEdge", PersonFollowingNothing::class, GroupSummary::class)
    }

    @Test
    fun `rejects an edge property the edge does not have`() {
        assertRejected<UnknownEdgeProperty>("UnknownEdgeProperty", PersonWithUnknownEdgeProperty::class, GroupWithUnknownEdgeProperty::class)
    }

    @Test
    fun `rejects an edge that does not touch the node the view is anchored at`() {
        assertRejected<EdgeNotIncidentOnRoot>("EdgeNotIncidentOnRoot", Place::class, Visited::class, GroupVisiting::class, GroupSummary::class)
    }

    @Test
    fun `rejects an explicit direction that contradicts the edge`() {
        assertRejected<EdgeNotIncidentOnRoot>("EdgeNotIncidentOnRoot", GroupWithContradictingDirection::class, MemberSummary::class)
    }

    @Test
    fun `rejects a self edge that is followed without a direction`() {
        assertRejected<AmbiguousDirection>("AmbiguousDirection", GroupWithAmbiguousParent::class, GroupSummary::class)
    }

    @Test
    fun `rejects a target view anchored at the wrong node`() {
        assertRejected<TargetLabelMismatch>("TargetLabelMismatch", PersonWithGroupsAsPeople::class, MemberSummary::class)
    }

    @Test
    fun `rejects a relation that leads to something that is not a view`() {
        assertRejected<RelationTargetNotAView>("RelationTargetNotAView", PersonWithGroupNodes::class)
    }

    @Test
    fun `rejects a view that contains itself`() {
        assertRejected<RecursiveView>("RecursiveView", GroupTree::class)
    }

    @Test
    fun `rejects edge properties in a view that is not the target of a relation`() {
        assertRejected<EdgePropertyViewNotUsedAsTarget>("EdgePropertyViewNotUsedAsTarget", OrphanEdgeView::class)
    }

    @Test
    fun `rejects edge properties in a view that is the target of more than one edge type`() {
        assertRejected<EdgePropertyViewSharedAcrossEdges>(
            "EdgePropertyViewSharedAcrossEdges",
            PersonWithTwoEdges::class,
            SharedEdgeView::class
        )
    }

    @Test
    fun `names the view that breaks the rule`() {
        val failure = assertRejected<UnknownNodeProperty>("UnknownNodeProperty", PersonWithNickname::class)

        assertEquals(PersonWithNickname::class, failure.view)
        assertEquals("UnknownNodeProperty", UnknownNodeProperty.REASON)
    }
}
