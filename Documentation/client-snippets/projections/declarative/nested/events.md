```kotlin title="Nested object events"
import io.cratis.chronicle.events.EventType

@EventType
data class SliceCreatedForNestedEvents(val name: String)

@EventType
data class CommandSetForNestedEvents(val name: String, val schema: String)

@EventType
data class CommandClearedForNestedEvents(val placeholder: Boolean = true)
```
