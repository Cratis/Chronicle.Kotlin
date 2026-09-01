```java title="Child lifecycle events"
import io.cratis.chronicle.events.EventType;

@EventType
record GroupCreatedForChildEvents(String name, String description) {}

@EventType
record UserAddedToGroupForChildEvents(String userId, String role) {}

@EventType
record UserRoleChangedForChildEvents(String userId, String role) {}

@EventType
record UserRemovedFromGroupForChildEvents(String userId) {}
```
