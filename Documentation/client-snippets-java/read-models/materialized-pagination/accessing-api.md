```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

record MaterializedPaginationOrder(String customerName, double total) {
    MaterializedPaginationOrder() {
        this("", 0.0);
    }
}

class MaterializedPaginationAccessingApi {
    private final IEventStore eventStore;

    MaterializedPaginationAccessingApi(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    // Reach through IEventStore, then the Java bridge for the Materialized API
    List<MaterializedPaginationOrder> getOrders() {
        return ReadModelsJavaBridge.getMaterializedInstances(eventStore.getReadModels(), MaterializedPaginationOrder.class, 0, 50);
    }
}
```
