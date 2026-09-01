```java title="Read a shared event property from every event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.readModels.ReadModel;

enum OrderStateFromEvery {
    New,
    Confirmed,
    Shipped
}

@EventType
record OrderConfirmedForEvery(OrderStateFromEvery status) {}

@EventType
record OrderShippedForEvery(OrderStateFromEvery status) {}

@ReadModel
@FromEvent(eventType = OrderConfirmedForEvery.class)
@FromEvent(eventType = OrderShippedForEvery.class)
class OrderStatusFromEvery {
    @FromEvery(property = "status")
    public OrderStateFromEvery currentStatus = OrderStateFromEvery.New;
}
```
