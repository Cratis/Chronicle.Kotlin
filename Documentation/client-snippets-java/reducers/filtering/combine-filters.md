```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-filtering-combine-order-placed")
record ReducersFilteringCombineOrderPlaced(double totalAmount) {}

@ReadModel
record ReducersFilteringPremiumFulfillmentTotals(int count, double total) {
    ReducersFilteringPremiumFulfillmentTotals() {
        this(0, 0.0);
    }
}

class ReducersFilteringCombineOrderService {
    private final IEventLog eventLog;

    ReducersFilteringCombineOrderService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void placePremiumOrder(String eventSourceId, double totalAmount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReducersFilteringCombineOrderPlaced(totalAmount),
            new AppendOptionsBuilder()
                .tag("premium")
                .eventSourceType("order")
                .eventStreamType("fulfillment")
                .build());
    }
}

@Reducer
@FilterEventsByTag("premium")
@EventSourceType("order")
@EventStreamType("fulfillment")
class ReducersFilteringPremiumFulfillmentTotalsReducer {
    ReducersFilteringPremiumFulfillmentTotals placed(ReducersFilteringCombineOrderPlaced event, ReducersFilteringPremiumFulfillmentTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        double total = current == null ? 0.0 : current.total();
        return new ReducersFilteringPremiumFulfillmentTotals(count + 1, total + event.totalAmount());
    }
}
```
