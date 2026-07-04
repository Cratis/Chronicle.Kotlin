```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import java.time.Instant;

@EventType(id = "reducers-getting-started-order-created")
record ReducersGettingStartedOrderCreated(String orderId) {}

@EventType(id = "reducers-getting-started-item-added-to-order")
record ReducersGettingStartedItemAddedToOrder(double price, int quantity) {}

@EventType(id = "reducers-getting-started-item-removed-from-order")
record ReducersGettingStartedItemRemovedFromOrder(double price, int quantity) {}

@Reducer
class ReducersGettingStartedOrderSummaryReducer {
    ReducersGettingStartedOrderSummary created(ReducersGettingStartedOrderCreated event) {
        return new ReducersGettingStartedOrderSummary(event.orderId(), 0.0, 0, Instant.now());
    }

    ReducersGettingStartedOrderSummary itemAdded(
        ReducersGettingStartedItemAddedToOrder event,
        ReducersGettingStartedOrderSummary current) {
        if (current == null) return null; // Skip if order not created yet

        return new ReducersGettingStartedOrderSummary(
            current.orderId(),
            current.totalAmount() + (event.price() * event.quantity()),
            current.itemCount() + event.quantity(),
            Instant.now());
    }

    ReducersGettingStartedOrderSummary itemRemoved(
        ReducersGettingStartedItemRemovedFromOrder event,
        ReducersGettingStartedOrderSummary current) {
        if (current == null) return null; // Skip if order not created yet

        return new ReducersGettingStartedOrderSummary(
            current.orderId(),
            current.totalAmount() - (event.price() * event.quantity()),
            current.itemCount() - event.quantity(),
            Instant.now());
    }
}
```
