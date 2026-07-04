```kotlin title="The event - an immutable fact"
import io.cratis.chronicle.events.EventType

@EventType(id = "TestEvent")
data class TestEvent(val message: String)
```
