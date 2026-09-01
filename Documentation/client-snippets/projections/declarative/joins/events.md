```kotlin
import io.cratis.chronicle.events.EventType

// User stream events
@EventType
data class DecJoinsUserCreated(val name: String, val email: String)

@EventType
data class DecJoinsUserAssignedToGroup(val userId: String, val groupId: String)

// Group stream events
@EventType
data class DecJoinsGroupCreated(val name: String, val description: String)

@EventType
data class DecJoinsGroupRenamed(val newName: String)
```
