```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ReducersFilteringMultiTagOrderPlaced(double totalAmount) {}

@ReadModel
record ReducersFilteringFastTrackOrderTotals(int count) {
    ReducersFilteringFastTrackOrderTotals() {
        this(0);
    }
}

@Reducer
@FilterEventsByTag("priority")
@FilterEventsByTag("express")
class ReducersFilteringFastTrackOrderTotalsReducer {
    ReducersFilteringFastTrackOrderTotals placed(ReducersFilteringMultiTagOrderPlaced event, ReducersFilteringFastTrackOrderTotals current, EventContext context) {
        int count = current == null ? 0 : current.count();
        return new ReducersFilteringFastTrackOrderTotals(count + 1);
    }
}
```
