```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.java.ReadModelsJavaBridge;

import java.util.List;

@ReadModel
class DesigningReadModelsCustomerListItem {
    private String id = "";
    private String name = "";

    public DesigningReadModelsCustomerListItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

class DesigningReadModelsCustomerListService {
    // Every instance in one call — read from the materialized store
    List<DesigningReadModelsCustomerListItem> getEveryInstance(EventStore store) {
        return ReadModelsJavaBridge.getInstances(store.getReadModels(), DesigningReadModelsCustomerListItem.class);
    }

    // One page of materialized instances, with paging done by the store
    List<DesigningReadModelsCustomerListItem> getPage(EventStore store) {
        return ReadModelsJavaBridge.getMaterializedInstances(store.getReadModels(), DesigningReadModelsCustomerListItem.class, 0, 20);
    }
}
```
