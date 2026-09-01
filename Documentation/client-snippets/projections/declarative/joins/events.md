```kotlin
import io.cratis.chronicle.events.EventType

// User stream events
@EventType(id = "dec-joins-user-created")
data class DecJoinsUserCreated(val name: String, val email: String)

@EventType(id = "dec-joins-user-assigned-to-group")
data class DecJoinsUserAssignedToGroup(val userId: String, val groupId: String)

// Group stream events
@EventType(id = "dec-joins-group-created")
data class DecJoinsGroupCreated(val name: String, val description: String)

@EventType(id = "dec-joins-group-renamed")
data class DecJoinsGroupRenamed(val newName: String)
```
