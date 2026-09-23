```java
import io.cratis.chronicle.java.BlockingEventStore;

class ComplianceReadModelsEmployeeService {
    private final BlockingEventStore eventStore;

    ComplianceReadModelsEmployeeService(BlockingEventStore eventStore) {
        this.eventStore = eventStore;
    }

    ComplianceReadModelsEmployee getEmployee(String id) {
        return eventStore.getReadModels().getInstanceByKey(ComplianceReadModelsEmployee.class, id);
    }
}
```
