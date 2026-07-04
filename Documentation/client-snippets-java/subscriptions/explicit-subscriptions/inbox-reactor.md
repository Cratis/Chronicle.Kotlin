```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "subscriptions-explicit-order-placed")
record SubscriptionsExplicitOrderPlaced(String orderId, double amount) {}

@Reactor
class SubscriptionsExplicitIncomingOrdersReactor {
    void orderPlaced(SubscriptionsExplicitOrderPlaced event, EventContext context) {
        handleIncomingOrder(event.orderId(), event.amount());
    }

    private void handleIncomingOrder(String id, double amount) {}
}
```
