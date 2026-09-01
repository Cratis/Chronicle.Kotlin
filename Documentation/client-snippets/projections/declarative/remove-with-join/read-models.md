```kotlin
import java.time.Instant

data class DecRemoveWithJoinUserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val registeredAt: Instant = Instant.EPOCH,
    val memberships: List<DecRemoveWithJoinGroupMembership> = emptyList()
)

data class DecRemoveWithJoinGroupMembership(
    val groupId: String = "",
    val groupName: String = "",
    val groupType: String = "",
    val joinedAt: Instant = Instant.EPOCH,
    val role: String = ""
)

data class DecRemoveWithJoinDeveloperProfile(
    val developerId: String = "",
    val name: String = "",
    val skills: List<String> = emptyList(),
    val onboardedAt: Instant = Instant.EPOCH,
    val currentProjects: List<DecRemoveWithJoinProjectAssignment> = emptyList()
)

data class DecRemoveWithJoinProjectAssignment(
    val projectId: String = "",
    val projectName: String = "",
    val priority: String = "",
    val deadline: Instant = Instant.EPOCH,
    val assignedAt: Instant = Instant.EPOCH,
    val role: String = "",
    val allocation: Int = 0
)
```
