```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class DecEventContextUserLoggedIn(val username: String)

@EventType
data class DecEventContextUserPerformedAction(val userId: String, val actionType: String)
```
