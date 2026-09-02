```java title="Multiple child collections"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.List;

@EventType
record GroupCreatedWithMultipleCollections(String name) {}

@EventType
record MemberAddedToGroup(String userId, String role) {}

@EventType
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
                    });
            })
            .children("tasks", GroupTaskInMultipleCollections.class, children -> {
                children
                    .identifiedBy("taskId")
                    .from(TaskAssignedToGroup.class, fb -> {
                        fb.usingKey("taskId");
                    });
            });
    }
}
```
