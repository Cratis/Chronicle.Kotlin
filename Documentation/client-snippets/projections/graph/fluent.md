```kotlin title="The same graph declared with a builder"
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.IGraphProjection
import io.cratis.chronicle.projections.graphs.IGraphProjectionBuilder

// The events, nodes and edge are the ones declared with annotations; a builder
// says what the events do to them without annotating the nodes.
@Graph("OrganizationByBuilder")
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
            }
            .edge(MemberOf::class, Person::class, Group::class) { edge ->
                edge.from(MemberAdded::class) { from ->
                    from.usingSourceKey("personId")
                        .usingTargetKey("groupId")
                        .set(MemberOf::since).toEventContextProperty("occurred")
                }.removedWith(MemberRemoved::class) { removal ->
                    removal.usingSourceKey("personId").usingTargetKey("groupId")
                }
            }
    }
}
```
