```kotlin
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@ReadModel
data class ReducersGettingStartedOrderSummary(
    val orderId: String = "",
    val totalAmount: Double = 0.0,
    val itemCount: Int = 0,
    val lastUpdated: Instant = Instant.EPOCH
)
```
