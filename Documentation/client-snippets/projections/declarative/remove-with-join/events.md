```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class DecRemoveWithJoinUserRegistered(val username: String, val email: String)

@EventType
data class DecRemoveWithJoinUserJoinedGroup(val userId: String, val groupId: String, val role: String)

@EventType
data class DecRemoveWithJoinUserLeftGroup(val userId: String, val groupId: String)

@EventType
data class DecRemoveWithJoinGroupCreated(val groupName: String, val groupType: String)

@EventType
data class DecRemoveWithJoinGroupDisbanded(val placeholder: Boolean = true)

@EventType
data class DecRemoveWithJoinDeveloperOnboarded(val name: String, val skills: List<String>)

@EventType
data class DecRemoveWithJoinDeveloperAssignedToProject(
    val developerId: String,
    val projectId: String,
    val role: String,
    val allocation: Int
)

@EventType
data class DecRemoveWithJoinDeveloperUnassignedFromProject(val developerId: String, val projectId: String)

@EventType
data class DecRemoveWithJoinProjectInitiated(
    val projectName: String,
    val priority: String,
    val deadline: java.time.Instant
)

@EventType
data class DecRemoveWithJoinProjectCancelled(val placeholder: Boolean = true)

@EventType
data class DecRemoveWithJoinProjectCompleted(val placeholder: Boolean = true)
```
