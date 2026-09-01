```kotlin title="Nested object events"
import io.cratis.chronicle.events.EventType

@EventType(id = "slice-created-for-nested-events")
data class SliceCreatedForNestedEvents(val name: String)

@EventType(id = "command-set-for-nested-events")
data class CommandSetForNestedEvents(val name: String, val schema: String)

@EventType(id = "command-cleared-for-nested-events")
data class CommandClearedForNestedEvents(val placeholder: Boolean = true)
```
