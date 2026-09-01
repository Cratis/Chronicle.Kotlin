```kotlin title="The event - an immutable fact"
import io.cratis.chronicle.events.EventType

@EventType
data class TestEvent(val message: String)
```
