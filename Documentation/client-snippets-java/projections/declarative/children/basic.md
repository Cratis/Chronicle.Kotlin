```java title="Projection with children"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.List;

@EventType
record GroupCreatedForChildren(String name, String description) {}

@EventType
record UserAddedToGroupForChildren(String userId, String role) {}

@EventType
record UserRoleChangedForChildren(String userId, String role) {}

record GroupForChildren(String name, String description, List<GroupMemberForChildren> members) {}

record GroupMemberForChildren(String userId, String role) {}

class GroupProjectionForChildren implements IProjectionFor<GroupForChildren> {
    @Override
    public void define(IProjectionBuilderFor<GroupForChildren> builder) {
        builder
            .from(GroupCreatedForChildren.class)
            .children("members", GroupMemberForChildren.class, children -> {
                children
                    .identifiedBy("userId")
                    .from(UserAddedToGroupForChildren.class, fb -> {
                        fb.usingKey("userId");
                    })
                    .from(UserRoleChangedForChildren.class, fb -> {
                        fb.usingKey("userId");
                    });
            });
    }
}
```
