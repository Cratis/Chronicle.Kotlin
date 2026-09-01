```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class ReactorOrderPlaced(
    val customerEmail: String,
    val totalAmount: Double
)
```
