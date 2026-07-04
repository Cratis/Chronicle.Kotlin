```kotlin
import io.cratis.chronicle.events.EventType

data class ModelingEventsOrderId(val value: String)
data class ModelingEventsMoney(val amount: Double, val currency: String)

// Nullable smell — "sometimes there's a discount, sometimes not"
@EventType(id = "modeling-events-order-placed-with-nullable-discount")
data class ModelingEventsOrderPlacedWithNullableDiscount(
    val id: ModelingEventsOrderId,
    val total: ModelingEventsMoney,
    val discount: ModelingEventsMoney?
)

// Two facts
@EventType(id = "modeling-events-order-placed")
data class ModelingEventsOrderPlaced(val id: ModelingEventsOrderId, val total: ModelingEventsMoney)

@EventType(id = "modeling-events-discount-applied")
data class ModelingEventsDiscountApplied(val id: ModelingEventsOrderId, val amount: ModelingEventsMoney)
```
