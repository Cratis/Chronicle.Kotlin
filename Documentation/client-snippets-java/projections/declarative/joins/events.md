```java
import io.cratis.chronicle.events.EventType;

// User stream events
@EventType
record DecJoinsUserCreated(String name, String email) {}

@EventType
record DecJoinsUserAssignedToGroup(String userId, String groupId) {}

// Group stream events
@EventType
record DecJoinsGroupCreated(String name, String description) {}

@EventType
record DecJoinsGroupRenamed(String newName) {}
```
