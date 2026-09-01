```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class EventsIndexTypeEmployeeRegistered(val firstName: String = "", val lastName: String = "")
```
