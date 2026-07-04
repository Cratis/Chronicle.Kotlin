```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "BookAdded")
data class BookAdded(val title: String, val isbn: String)
```
