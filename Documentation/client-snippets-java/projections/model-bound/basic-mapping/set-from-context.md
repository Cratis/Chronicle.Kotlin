```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "order-placed-for-audit")
record OrderPlacedForAudit(String customerName) {}

@ReadModel
@FromEvent(eventType = OrderPlacedForAudit.class)
class AuditedOrder {
    @SetFrom(propertyPath = "customerName", eventType = OrderPlacedForAudit.class)
    public String customerName = "";

    @SetFromContext(eventType = OrderPlacedForAudit.class, contextProperty = "occurred")
    public String orderedAt = "";
}
```
