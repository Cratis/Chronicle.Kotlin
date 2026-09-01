```java
import io.cratis.chronicle.events.EventType;

import java.time.Instant;
import java.util.List;

@EventType(id = "dec-remove-with-join-user-registered")
record DecRemoveWithJoinUserRegistered(String username, String email) {}

@EventType(id = "dec-remove-with-join-user-joined-group")
record DecRemoveWithJoinUserJoinedGroup(String userId, String groupId, String role) {}

@EventType(id = "dec-remove-with-join-user-left-group")
record DecRemoveWithJoinUserLeftGroup(String userId, String groupId) {}

@EventType(id = "dec-remove-with-join-group-created")
record DecRemoveWithJoinGroupCreated(String groupName, String groupType) {}

@EventType(id = "dec-remove-with-join-group-disbanded")
record DecRemoveWithJoinGroupDisbanded() {}

@EventType(id = "dec-remove-with-join-developer-onboarded")
record DecRemoveWithJoinDeveloperOnboarded(String name, List<String> skills) {}

@EventType(id = "dec-remove-with-join-developer-assigned-to-project")
record DecRemoveWithJoinDeveloperAssignedToProject(
    String developerId,
    String projectId,
    String role,
    int allocation
) {}

@EventType(id = "dec-remove-with-join-developer-unassigned-from-project")
record DecRemoveWithJoinDeveloperUnassignedFromProject(String developerId, String projectId) {}

@EventType(id = "dec-remove-with-join-project-initiated")
record DecRemoveWithJoinProjectInitiated(String projectName, String priority, Instant deadline) {}

@EventType(id = "dec-remove-with-join-project-cancelled")
record DecRemoveWithJoinProjectCancelled() {}

@EventType(id = "dec-remove-with-join-project-completed")
record DecRemoveWithJoinProjectCompleted() {}
```
