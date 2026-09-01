```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.Reducer;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterByStreamTypeShipmentSent(double shippingCost) {}

record FilterByStreamTypeShippingTotals(double shippingCost) {}

@EventStreamType("shipping")
@Reducer
class FilterByStreamTypeShippingTotalsReducer {
    public FilterByStreamTypeShippingTotals shipmentSent(FilterByStreamTypeShipmentSent event, FilterByStreamTypeShippingTotals current) {
        double currentCost = current != null ? current.shippingCost() : 0.0;
        return new FilterByStreamTypeShippingTotals(currentCost + event.shippingCost());
    }
}

class EventsFilteringByEventStreamTypeReducer {
    AppendResult send(EventStore store, String eventSourceId, double shippingCost) {
        AppendOptions options = new AppendOptionsBuilder().eventStreamType("shipping").build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterByStreamTypeShipmentSent(shippingCost), options);
    }
}
```
