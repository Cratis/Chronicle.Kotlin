```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-filtering-shipment-sent")
record ReducersFilteringShipmentSent(double shippingCost) {}

@ReadModel
record ReducersFilteringShippingTotals(int count, double totalCost) {
    ReducersFilteringShippingTotals() {
        this(0, 0.0);
    }
}

class ReducersFilteringShippingService {
    private final IEventLog eventLog;

    ReducersFilteringShippingService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void send(String eventSourceId, double shippingCost) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReducersFilteringShipmentSent(shippingCost),
            new AppendOptionsBuilder().eventStreamType("shipping").build());
    }
}

@Reducer
@EventStreamType("shipping")
class ReducersFilteringShippingTotalsReducer {
    ReducersFilteringShippingTotals sent(ReducersFilteringShipmentSent event, ReducersFilteringShippingTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        double totalCost = current == null ? 0.0 : current.totalCost();
        return new ReducersFilteringShippingTotals(count + 1, totalCost + event.shippingCost());
    }
}
```
