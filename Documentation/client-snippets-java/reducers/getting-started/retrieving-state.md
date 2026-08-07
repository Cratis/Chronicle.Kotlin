```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;

class ReducersGettingStartedOrderService {
    private final BlockingEventStore store;

    ReducersGettingStartedOrderService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    ReducersGettingStartedOrderSummary getOrderSummary(String orderId) {
        return store.getReadModels()
            .getInstanceByKey(ReducersGettingStartedOrderSummary.class, orderId);
    }
}
```
