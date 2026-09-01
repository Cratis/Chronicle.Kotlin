```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-counters-order-placed")
class MbCountersOrderPlaced {}

@EventType(id = "mb-counters-order-cancelled")
class MbCountersOrderCancelled {}

@ReadModel
@FromEvent(eventType = MbCountersOrderPlaced.class)
@FromEvent(eventType = MbCountersOrderCancelled.class)
class MbCountersEventMetrics {
    @Count(eventType = MbCountersOrderPlaced.class)
    public int totalOrders = 0;

    @Count(eventType = MbCountersOrderCancelled.class)
    public int cancelledOrders = 0;
}
```
