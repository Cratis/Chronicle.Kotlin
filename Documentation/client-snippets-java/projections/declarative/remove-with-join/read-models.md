```java
import java.time.Instant;
import java.util.List;

record DecRemoveWithJoinUserProfile(
    String userId,
    String username,
    String email,
    Instant registeredAt,
    List<DecRemoveWithJoinGroupMembership> memberships
) {}

record DecRemoveWithJoinGroupMembership(
    String groupId,
    String groupName,
    String groupType,
    Instant joinedAt,
    String role
) {}

record DecRemoveWithJoinDeveloperProfile(
    String developerId,
    String name,
    List<String> skills,
    Instant onboardedAt,
    List<DecRemoveWithJoinProjectAssignment> currentProjects
) {}

record DecRemoveWithJoinProjectAssignment(
    String projectId,
    String projectName,
    String priority,
    Instant deadline,
    Instant assignedAt,
    String role,
    int allocation
) {}
```
