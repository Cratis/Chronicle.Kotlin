```java
import io.cratis.chronicle.events.EventType;

record ModelingEventsOrderId(String value) {}
record ModelingEventsMoney(double amount, String currency) {}

// Nullable smell — "sometimes there's a discount, sometimes not"
@EventType
record ModelingEventsOrderPlacedWithNullableDiscount(
    ModelingEventsOrderId id,
    ModelingEventsMoney total,
    ModelingEventsMoney discount) {}

// Two facts
@EventType
record ModelingEventsOrderPlaced(ModelingEventsOrderId id, ModelingEventsMoney total) {}

@EventType
record ModelingEventsDiscountApplied(ModelingEventsOrderId id, ModelingEventsMoney amount) {}
```
