```java title="Multiple child collections"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.List;

@EventType(id = "group-created-with-multiple-collections")
record GroupCreatedWithMultipleCollections(String name) {}

@EventType(id = "member-added-to-group")
record MemberAddedToGroup(String userId, String role) {}

@EventType(id = "task-assigned-to-group")
record TaskAssignedToGroup(String taskId, String title) {}

record GroupWithMultipleCollections(
    String name,
    List<GroupMemberInMultipleCollections> members,
    List<GroupTaskInMultipleCollections> tasks) {}

record GroupMemberInMultipleCollections(String userId, String role) {}

record GroupTaskInMultipleCollections(String taskId, String title) {}

class GroupWithMultipleCollectionsProjection implements IProjectionFor<GroupWithMultipleCollections> {
    @Override
    public void define(IProjectionBuilderFor<GroupWithMultipleCollections> builder) {
        builder
            .from(GroupCreatedWithMultipleCollections.class)
            .children("members", GroupMemberInMultipleCollections.class, children -> {
                children
                    .identifiedBy("userId")
                    .from(MemberAddedToGroup.class, fb -> {
                        fb.usingKey("userId");
                        return null; // Java lambda returning Unit
                    });
                return null; // Java lambda returning Unit
            })
            .children("tasks", GroupTaskInMultipleCollections.class, children -> {
                children
                    .identifiedBy("taskId")
                    .from(TaskAssignedToGroup.class, fb -> {
                        fb.usingKey("taskId");
                        return null; // Java lambda returning Unit
                    });
                return null; // Java lambda returning Unit
            });
    }
}
```
