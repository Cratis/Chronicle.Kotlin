```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-filtering-tag-vs-filter-order-placed")
record ReducersFilteringTagVsFilterOrderPlaced(double totalAmount) {}

@ReadModel
record ReducersFilteringTagVsFilterTotals(int count, double total) {
    ReducersFilteringTagVsFilterTotals() {
        this(0, 0.0);
    }
}

// These labels appear on the reducer definition - they do not affect dispatch
@Reducer
@Tag("reporting")
@Tag("premium")
class ReducersFilteringLabeledFulfillmentTotalsReducer {
    ReducersFilteringTagVsFilterTotals placed(ReducersFilteringTagVsFilterOrderPlaced event, ReducersFilteringTagVsFilterTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        double total = current == null ? 0.0 : current.total();
        return new ReducersFilteringTagVsFilterTotals(count + 1, total + event.totalAmount());
    }
}

// These filter which events are dispatched to the reducer
@Reducer
@FilterEventsByTag("premium")
@EventSourceType("order")
class ReducersFilteringFilteredFulfillmentTotalsReducer {
    ReducersFilteringTagVsFilterTotals placed(ReducersFilteringTagVsFilterOrderPlaced event, ReducersFilteringTagVsFilterTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        double total = current == null ? 0.0 : current.total();
        return new ReducersFilteringTagVsFilterTotals(count + 1, total + event.totalAmount());
    }
}
```
