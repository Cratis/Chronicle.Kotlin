```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

class MaterializedPaginationPagination {
    private final IEventStore eventStore;

    MaterializedPaginationPagination(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void getPages() {
        // First page of 20
        var page1 = ReadModelsJavaBridge.getMaterializedInstances(eventStore.getReadModels(), MaterializedPaginationOrder.class, 0, 20);
        System.out.println("Page 1: " + page1.size() + " orders");

        // Second page of 20
        var page2 = ReadModelsJavaBridge.getMaterializedInstances(eventStore.getReadModels(), MaterializedPaginationOrder.class, 20, 20);
        System.out.println("Page 2: " + page2.size() + " orders");

        // Third page of 20
        var page3 = ReadModelsJavaBridge.getMaterializedInstances(eventStore.getReadModels(), MaterializedPaginationOrder.class, 40, 20);
        System.out.println("Page 3: " + page3.size() + " orders");
    }
}
```
