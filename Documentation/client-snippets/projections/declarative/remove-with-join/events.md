```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "dec-remove-with-join-user-registered")
data class DecRemoveWithJoinUserRegistered(val username: String, val email: String)

@EventType(id = "dec-remove-with-join-user-joined-group")
data class DecRemoveWithJoinUserJoinedGroup(val userId: String, val groupId: String, val role: String)

@EventType(id = "dec-remove-with-join-user-left-group")
data class DecRemoveWithJoinUserLeftGroup(val userId: String, val groupId: String)

@EventType(id = "dec-remove-with-join-group-created")
data class DecRemoveWithJoinGroupCreated(val groupName: String, val groupType: String)

@EventType(id = "dec-remove-with-join-group-disbanded")
data class DecRemoveWithJoinGroupDisbanded(val placeholder: Boolean = true)

@EventType(id = "dec-remove-with-join-developer-onboarded")
data class DecRemoveWithJoinDeveloperOnboarded(val name: String, val skills: List<String>)

@EventType(id = "dec-remove-with-join-developer-assigned-to-project")
data class DecRemoveWithJoinDeveloperAssignedToProject(
    val developerId: String,
    val projectId: String,
    val role: String,
    val allocation: Int
)

@EventType(id = "dec-remove-with-join-developer-unassigned-from-project")
data class DecRemoveWithJoinDeveloperUnassignedFromProject(val developerId: String, val projectId: String)

@EventType(id = "dec-remove-with-join-project-initiated")
data class DecRemoveWithJoinProjectInitiated(
    val projectName: String,
    val priority: String,
    val deadline: java.time.Instant
)

@EventType(id = "dec-remove-with-join-project-cancelled")
data class DecRemoveWithJoinProjectCancelled(val placeholder: Boolean = true)

@EventType(id = "dec-remove-with-join-project-completed")
data class DecRemoveWithJoinProjectCompleted(val placeholder: Boolean = true)
```
