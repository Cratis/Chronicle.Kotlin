```java title="Child lifecycle events"
import io.cratis.chronicle.events.EventType;

@EventType(id = "group-created-for-child-events")
record GroupCreatedForChildEvents(String name, String description) {}

@EventType(id = "user-added-to-group-for-child-events")
record UserAddedToGroupForChildEvents(String userId, String role) {}

@EventType(id = "user-role-changed-for-child-events")
record UserRoleChangedForChildEvents(String userId, String role) {}

@EventType(id = "user-removed-from-group-for-child-events")
record UserRemovedFromGroupForChildEvents(String userId) {}
```
