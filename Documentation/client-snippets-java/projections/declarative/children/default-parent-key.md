```java title="Default parent key"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.List;

@EventType(id = "group-created-with-default-parent-key")
record GroupCreatedWithDefaultParentKey(String name) {}

record GroupWithDefaultParentKey(String name, List<GroupMemberWithDefaultParentKey> members) {}

record GroupMemberWithDefaultParentKey(String userId, String role) {}

class GroupWithDefaultParentKeyProjection implements IProjectionFor<GroupWithDefaultParentKey> {
    @Override
    public void define(IProjectionBuilderFor<GroupWithDefaultParentKey> builder) {
        builder
            .from(GroupCreatedWithDefaultParentKey.class)
            .children("members", GroupMemberWithDefaultParentKey.class, children -> {
                children
                    .identifiedBy("userId")
                    .from(UserAddedWithDefaultParentKey.class, fb -> {
                        fb.usingKey("userId");
                        return null; // Java lambda returning Unit
                    });
                return null; // Java lambda returning Unit
            });
    }
}
```
