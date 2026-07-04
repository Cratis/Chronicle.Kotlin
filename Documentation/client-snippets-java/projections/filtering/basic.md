```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@EventType
record FilteringOrderPlaced(String customerId, BigDecimal totalAmount) {}

@EventType
record FilteringOrderShipped(OffsetDateTime shippedAt) {}

@ReadModel
@FromEvent(eventType = FilteringOrderPlaced.class)
@FromEvent(eventType = FilteringOrderShipped.class)
class FilteringOrderSummary {
    public String customerId = "";
    public BigDecimal totalAmount = BigDecimal.ZERO;
    public OffsetDateTime shippedAt = null;
}
```
