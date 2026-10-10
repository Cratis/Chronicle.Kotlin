```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class BookRegistered(val title: String, val isbn: String)

@EventType
data class BookBorrowed(val memberName: String)

@EventType
class BookReturned
```
