```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class BookAdded(val title: String, val isbn: String)
```
