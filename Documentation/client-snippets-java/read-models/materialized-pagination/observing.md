```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import kotlinx.coroutines.Job;

record MaterializedPaginationProduct(String name, double price) {
}

class MaterializedPaginationObserving {
    private final IEventStore eventStore;

    MaterializedPaginationObserving(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void run() {
        Job subscription = ReadModelsJavaBridge.observeMaterializedInstances(
            eventStore.getReadModels(),
            MaterializedPaginationProduct.class,
            0,
            50,
            products -> {
                // Called whenever the stored instances change
                System.out.println("Products updated: " + products.size() + " in view");
            });

        // Cancel when done to release the change stream
        subscription.cancel(null);
    }
}
```
