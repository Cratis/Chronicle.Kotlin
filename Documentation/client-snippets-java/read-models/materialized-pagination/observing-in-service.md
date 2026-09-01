```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import kotlinx.coroutines.Job;

import java.util.List;

/**
 * Observes a live paginated window of materialized instances until {@code close()} releases it.
 */
class ProductDashboard implements AutoCloseable {
    private final Job subscription;

    ProductDashboard(IEventStore store) {
        subscription = ReadModelsJavaBridge.observeMaterializedInstances(
            store.getReadModels(),
            MaterializedPaginationProduct.class,
            0,
            100,
            this::updateView);
    }

    private void updateView(List<MaterializedPaginationProduct> products) {
        // ...
    }

    @Override
    public void close() {
        subscription.cancel(null);
    }
}
```
