```kotlin
import io.cratis.chronicle.events.EventType

data class ModelingEventsOrderId(val value: String)
data class ModelingEventsMoney(val amount: Double, val currency: String)

// Nullable smell — "sometimes there's a discount, sometimes not"
@EventType
data class ModelingEventsOrderPlacedWithNullableDiscount(
    val id: ModelingEventsOrderId,
    val total: ModelingEventsMoney,
    val discount: ModelingEventsMoney?
)

// Two facts
@EventType
data class ModelingEventsOrderPlaced(val id: ModelingEventsOrderId, val total: ModelingEventsMoney)

@EventType
data class ModelingEventsDiscountApplied(val id: ModelingEventsOrderId, val amount: ModelingEventsMoney)
```
