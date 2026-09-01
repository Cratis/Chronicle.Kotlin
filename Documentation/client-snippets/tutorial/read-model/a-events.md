```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class BookBorrowed(val memberName: String)

@EventType
class BookReturned
```
