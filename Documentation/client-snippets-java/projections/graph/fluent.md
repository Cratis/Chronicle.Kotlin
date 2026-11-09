```java title="The same graph declared with a builder"
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.IGraphProjection;
import io.cratis.chronicle.projections.graphs.IGraphProjectionBuilder;

// The events, nodes and edge are the ones declared with annotations; a builder
// says what the events do to them without annotating the nodes.
@Graph("OrganizationByBuilder")
class OrganizationGraph implements IGraphProjection {
    @Override
    public void define(IGraphProjectionBuilder builder) {
        builder
            .node(Person.class, node -> {
                node.identifiedBy("personId")
                    .from(PersonRegistered.class)
                    .removedWith(PersonRemoved.class, removal -> {
                        removal.detach();
                    });
            })
            .node(Group.class, node -> {
                node.identifiedBy("groupId")
                    .from(GroupCreated.class);
            })
            .edge(MemberOf.class, Person.class, Group.class, edge -> {
                edge.from(MemberAdded.class, from -> {
                        from.usingSourceKey("personId")
                            .usingTargetKey("groupId")
                            .set("since").toEventContextProperty("occurred");
                    })
                    .removedWith(MemberRemoved.class, removal -> {
                        removal.usingSourceKey("personId").usingTargetKey("groupId");
                    });
            });
    }
}
```
