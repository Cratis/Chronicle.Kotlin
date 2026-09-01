```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;

import java.util.List;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterByTagOrderPlaced(double totalAmount) {}

record FilterByTagPriorityOrderTotals(double totalAmount) {}

@FilterEventsByTag("priority")
@Reducer
class FilterByTagPriorityOrderTotalsReducer {
    public FilterByTagPriorityOrderTotals orderPlaced(FilterByTagOrderPlaced event, FilterByTagPriorityOrderTotals current) {
        double currentTotal = current != null ? current.totalAmount() : 0.0;
        return new FilterByTagPriorityOrderTotals(currentTotal + event.totalAmount());
    }
}

class EventsFilteringByTagReducer {
    AppendResult placePriorityOrder(EventStore store, String eventSourceId, double totalAmount) {
        AppendOptions options = new AppendOptionsBuilder().tags(List.of("priority")).build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterByTagOrderPlaced(totalAmount), options);
    }
}
```
