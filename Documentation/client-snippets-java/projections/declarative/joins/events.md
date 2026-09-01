```java
import io.cratis.chronicle.events.EventType;

// User stream events
@EventType(id = "dec-joins-user-created")
record DecJoinsUserCreated(String name, String email) {}

@EventType(id = "dec-joins-user-assigned-to-group")
record DecJoinsUserAssignedToGroup(String userId, String groupId) {}

// Group stream events
@EventType(id = "dec-joins-group-created")
record DecJoinsGroupCreated(String name, String description) {}

@EventType(id = "dec-joins-group-renamed")
record DecJoinsGroupRenamed(String newName) {}
```
