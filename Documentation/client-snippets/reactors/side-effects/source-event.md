```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class BookReserved(val memberId: String, val isbn: String)
```
