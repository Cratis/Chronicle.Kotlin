```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class PagedEndpointOrder {
    private String status = "";
    private double total = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}

class ReadModelsMaterializedPaginationPagedEndpoint {
    // A paged read suitable for backing a list endpoint - only the requested page is loaded.
    List<PagedEndpointOrder> getOrders(EventStore store, int page, int pageSize) {
        return ReadModelsJavaBridge.getMaterializedInstances(
            store.getReadModels(), PagedEndpointOrder.class, page * pageSize, pageSize);
    }
}
```
