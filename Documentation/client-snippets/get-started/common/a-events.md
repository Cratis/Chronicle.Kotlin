```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "GetStartedBookAdded")
data class GetStartedBookAdded(val title: String, val isbn: String)

@EventType(id = "GetStartedBookBorrowed")
data class GetStartedBookBorrowed(val memberName: String)

@EventType(id = "GetStartedBookReturned")
class GetStartedBookReturned
```
