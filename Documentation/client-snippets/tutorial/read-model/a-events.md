```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "BookBorrowed")
data class BookBorrowed(val memberName: String)

@EventType(id = "BookReturned")
class BookReturned
```
