```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record ReducersGettingStartedAttributeOrderSummary(String orderId) {
    ReducersGettingStartedAttributeOrderSummary() {
        this("");
    }
}

@Reducer(id = "order-summary", eventSequence = "outbox", isActive = false)
class ReducersGettingStartedAttributeOrderSummaryReducer {
    public ReducersGettingStartedAttributeOrderSummary on(ReducersGettingStartedAttributeOrderPlaced event) {
        return new ReducersGettingStartedAttributeOrderSummary(event.orderId());
    }
}

@EventType
record ReducersGettingStartedAttributeOrderPlaced(String orderId) {}
```
