```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-filtering-by-tag-order-placed")
record ReducersFilteringByTagOrderPlaced(double totalAmount) {}

@ReadModel
record ReducersFilteringPriorityOrderTotals(int count, double total) {
    ReducersFilteringPriorityOrderTotals() {
        this(0, 0.0);
    }
}

class ReducersFilteringByTagOrderService {
    private final IEventLog eventLog;

    ReducersFilteringByTagOrderService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void placePriorityOrder(String eventSourceId, double totalAmount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReducersFilteringByTagOrderPlaced(totalAmount),
            new AppendOptionsBuilder().tag("priority").build());
    }
}

@Reducer
@FilterEventsByTag("priority")
class ReducersFilteringPriorityOrderTotalsReducer {
    ReducersFilteringPriorityOrderTotals placed(ReducersFilteringByTagOrderPlaced event, ReducersFilteringPriorityOrderTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        double total = current == null ? 0.0 : current.total();
        return new ReducersFilteringPriorityOrderTotals(count + 1, total + event.totalAmount());
    }
}
```
