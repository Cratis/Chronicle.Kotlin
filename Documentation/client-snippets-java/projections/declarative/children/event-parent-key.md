```java title="Parent key from event content"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.List;

@EventType
record GroupCreatedWithEventParentKey(String name) {}

record GroupWithEventParentKey(String name, List<GroupMemberWithEventParentKey> members) {}

record GroupMemberWithEventParentKey(String userId, String role) {}

class GroupWithEventParentKeyProjection implements IProjectionFor<GroupWithEventParentKey> {
    @Override
    public void define(IProjectionBuilderFor<GroupWithEventParentKey> builder) {
        builder
            .from(GroupCreatedWithEventParentKey.class)
            .children("members", GroupMemberWithEventParentKey.class, children -> {
                children
                    .identifiedBy("userId")
                    .from(UserAddedWithEventParentKey.class, fb -> {
                        fb.usingParentKey("groupId");
                        fb.usingKey("userId");
                        return null; // Java lambda returning Unit
                    });
                return null; // Java lambda returning Unit
            });
    }
}
```
