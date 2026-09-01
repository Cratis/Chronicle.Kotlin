```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "dec-event-context-user-logged-in")
data class DecEventContextUserLoggedIn(val username: String)

@EventType(id = "dec-event-context-user-performed-action")
data class DecEventContextUserPerformedAction(val userId: String, val actionType: String)
```
