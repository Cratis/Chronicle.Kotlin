```kotlin title="Child lifecycle events"
import io.cratis.chronicle.events.EventType

@EventType
data class GroupCreatedForChildEvents(val name: String, val description: String)

@EventType
data class UserAddedToGroupForChildEvents(val userId: String, val role: String)

@EventType
data class UserRoleChangedForChildEvents(val userId: String, val role: String)

@EventType
data class UserRemovedFromGroupForChildEvents(val userId: String)
```
