```kotlin title="Child lifecycle events"
import io.cratis.chronicle.events.EventType

@EventType(id = "group-created-for-child-events")
data class GroupCreatedForChildEvents(val name: String, val description: String)

@EventType(id = "user-added-to-group-for-child-events")
data class UserAddedToGroupForChildEvents(val userId: String, val role: String)

@EventType(id = "user-role-changed-for-child-events")
data class UserRoleChangedForChildEvents(val userId: String, val role: String)

@EventType(id = "user-removed-from-group-for-child-events")
data class UserRemovedFromGroupForChildEvents(val userId: String)
```
