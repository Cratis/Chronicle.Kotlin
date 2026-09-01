```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingReadModelsScenarioReducerExample {

    @EventType
    record OrderCreated(String orderId) {
    }

    @EventType
    record ItemAdded(double price) {
    }

    @ReadModel
    record OrderSummary(String orderId, double total) {
    }

    @Reducer
    static class OrderSummaryReducer {
        OrderSummary orderCreated(OrderCreated event) {
            return new OrderSummary(event.orderId(), 0.0);
        }

        OrderSummary itemAdded(ItemAdded event, OrderSummary current) {
            return new OrderSummary(current.orderId(), current.total() + event.price());
        }
    }

    @Test
    void eachEventFoldsIntoTheRunningTotal() {
        var reducer = new OrderSummaryReducer();

        var summary = reducer.orderCreated(new OrderCreated("order-1"));
        summary = reducer.itemAdded(new ItemAdded(9.99), summary);
        summary = reducer.itemAdded(new ItemAdded(4.50), summary);

        assertEquals(14.49, summary.total(), 0.001);
    }
}
```
