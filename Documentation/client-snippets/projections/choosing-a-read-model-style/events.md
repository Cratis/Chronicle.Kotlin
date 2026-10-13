```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType

@EventType
data class ChoosingStyleBookRegistered(val title: String, val isbn: String)

@EventType
data class ChoosingStyleBookBorrowed(@property:Pii val memberName: String)

@EventType
class ChoosingStyleBookReturned
```
