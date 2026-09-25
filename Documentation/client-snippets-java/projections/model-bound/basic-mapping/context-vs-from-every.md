```java title="Specific context vs every event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record OrderPlacedForLifecycle(String customerName) {}

@EventType
record OrderShippedForLifecycle(String trackingNumber) {}

@ReadModel
@FromEvent(eventType = OrderPlacedForLifecycle.class)
@FromEvent(eventType = OrderShippedForLifecycle.class)
class OrderLifecycle {
    @SetFromContext(eventType = OrderPlacedForLifecycle.class, contextProperty = "occurred")
    public String placedAt = "";

    @SetFromContext(eventType = OrderShippedForLifecycle.class, contextProperty = "occurred")
    public String shippedAt;

    @FromEvery(contextProperty = "occurred")
    public String lastModified = "";
}
```
