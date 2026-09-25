```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.Passive;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record DesigningReadModelsCustomerNamed(String name) {}

// Nothing materializes this read model: every read computes it from the event log.
@Passive
@ReadModel
@FromEvent(eventType = DesigningReadModelsCustomerNamed.class)
class DesigningReadModelsCustomerDetail {
    public String name = "";
}

class DesigningReadModelsCustomerDetailService {
    private final EventStore eventStore;

    DesigningReadModelsCustomerDetailService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    DesigningReadModelsCustomerDetail getDetail(String customerId) {
        return ReadModelsJavaBridge.getInstanceByKey(
            eventStore.getReadModels(), DesigningReadModelsCustomerDetail.class, customerId);
    }
}
```
