```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

class MaterializedPaginationBasicUsage {
    private final IEventStore eventStore;

    MaterializedPaginationBasicUsage(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    List<MaterializedPaginationOrder> getOrders() {
        return ReadModelsJavaBridge.getMaterializedInstances(eventStore.getReadModels(), MaterializedPaginationOrder.class, 0, 50);
    }
}
```
