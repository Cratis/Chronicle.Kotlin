```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;

record DesigningReadModelsCustomerDetail(String id, String name) {}

class DesigningReadModelsCustomerDetailService {
    private final BlockingEventStore store;

    DesigningReadModelsCustomerDetailService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    DesigningReadModelsCustomerDetail getDetail(String customerId) {
        return store.getReadModels()
            .getInstanceByKey(DesigningReadModelsCustomerDetail.class, customerId);
    }
}
```
