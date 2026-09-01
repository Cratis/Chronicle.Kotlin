```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.projections.ProjectionQueryResult;

import java.util.List;

record PdlOrderSummary(String orderId) {}

class PdlOrderQueryService {
    private final EventStore store;

    PdlOrderQueryService(EventStore store) {
        this.store = store;
    }

    List<String> getOrderSummaries() {
        ProjectionQueryResult result = ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection OrderSummary\n  from OrderPlaced"
        );

        if (result instanceof ProjectionQueryResult.Projected projected) {
            // Raw JSON documents — instancesOf() takes a Kotlin KClass and has no Java-callable overload.
            return projected.getEntries();
        }
        return List.of();
    }
}
```
