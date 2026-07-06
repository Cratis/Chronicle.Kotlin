```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "ReactorOrderPlaced")
data class ReactorOrderPlaced(
    val customerEmail: String,
    val totalAmount: Double
)
```
