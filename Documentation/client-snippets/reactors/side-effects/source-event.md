```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "side-effects-book-reserved-source")
data class BookReserved(val memberId: String, val isbn: String)
```
