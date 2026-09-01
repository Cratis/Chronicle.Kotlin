```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class GetStartedBookAdded(val title: String, val isbn: String)

@EventType
data class GetStartedBookBorrowed(val memberName: String)

@EventType
class GetStartedBookReturned
```
