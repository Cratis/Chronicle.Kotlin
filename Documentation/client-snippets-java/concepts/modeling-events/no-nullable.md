```java
import io.cratis.chronicle.events.EventType;

record ModelingEventsOrderId(String value) {}
record ModelingEventsMoney(double amount, String currency) {}

// Nullable smell — "sometimes there's a discount, sometimes not"
@EventType(id = "modeling-events-order-placed-with-nullable-discount")
record ModelingEventsOrderPlacedWithNullableDiscount(
    ModelingEventsOrderId id,
    ModelingEventsMoney total,
    ModelingEventsMoney discount) {}

// Two facts
@EventType(id = "modeling-events-order-placed")
record ModelingEventsOrderPlaced(ModelingEventsOrderId id, ModelingEventsMoney total) {}

@EventType(id = "modeling-events-discount-applied")
record ModelingEventsDiscountApplied(ModelingEventsOrderId id, ModelingEventsMoney amount) {}
```
