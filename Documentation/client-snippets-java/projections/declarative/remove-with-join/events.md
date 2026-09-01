```java
import io.cratis.chronicle.events.EventType;

import java.time.Instant;
import java.util.List;

@EventType
record DecRemoveWithJoinUserRegistered(String username, String email) {}

@EventType
record DecRemoveWithJoinUserJoinedGroup(String userId, String groupId, String role) {}

@EventType
record DecRemoveWithJoinUserLeftGroup(String userId, String groupId) {}

@EventType
record DecRemoveWithJoinGroupCreated(String groupName, String groupType) {}

@EventType
record DecRemoveWithJoinGroupDisbanded() {}

@EventType
record DecRemoveWithJoinDeveloperOnboarded(String name, List<String> skills) {}

@EventType
record DecRemoveWithJoinDeveloperAssignedToProject(
    String developerId,
    String projectId,
    String role,
    int allocation
) {}

@EventType
record DecRemoveWithJoinDeveloperUnassignedFromProject(String developerId, String projectId) {}

@EventType
record DecRemoveWithJoinProjectInitiated(String projectName, String priority, Instant deadline) {}

@EventType
record DecRemoveWithJoinProjectCancelled() {}

@EventType
record DecRemoveWithJoinProjectCompleted() {}
```
