```kotlin
import io.cratis.chronicle.events.EventType

data class ModelingEventsAddress(val street: String, val city: String)

// A fact that happened
@EventType
data class ModelingEventsAddressChanged(val address: ModelingEventsAddress)

// An intent (that's a command) or a state blob (that's a read model) — not an event
@EventType
data class ModelingEventsUpdateAddress(val address: ModelingEventsAddress)
```
