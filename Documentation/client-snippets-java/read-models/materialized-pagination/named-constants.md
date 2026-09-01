```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class NamedConstantsOrder {
    private String status = "";
    private double total = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}

class ReadModelsMaterializedPaginationNamedConstants {
    // skip: 0, take: 50 are the Kotlin client's built-in defaults - repeat them explicitly since
    // the Java bridge has no default-argument support.
    List<NamedConstantsOrder> getOrders(EventStore store) {
        return ReadModelsJavaBridge.getMaterializedInstances(store.getReadModels(), NamedConstantsOrder.class, 0, 50);
    }
}
```
