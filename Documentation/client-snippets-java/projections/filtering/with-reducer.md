```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reducer;

record FilteringPremiumOrderTotals(int count, double total) {}

@Reducer
@FilterEventsByTag("premium")
class FilteringPremiumOrderTotalsReducer {
    FilteringPremiumOrderTotals placed(
        FilteringWithReactorOrderPlaced event,
        FilteringPremiumOrderTotals current,
        EventContext context
    ) {
        int count = current == null ? 0 : current.count();
        double total = current == null ? 0.0 : current.total();
        return new FilteringPremiumOrderTotals(count + 1, total + event.totalAmount());
    }
}
```
