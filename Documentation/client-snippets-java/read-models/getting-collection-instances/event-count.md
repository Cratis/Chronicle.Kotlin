```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

@ReadModel
record EventCountOrder(String status, double total) {
}

class EventCountOrders {
    /**
     * Caps the replay to the first 1,000 events - faster, but can return incomplete state if the
     * cap cuts off events that matter.
     */
    static List<EventCountOrder> replayCappedOrders(IEventStore store) {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), EventCountOrder.class, 1_000L);
    }
}
```
