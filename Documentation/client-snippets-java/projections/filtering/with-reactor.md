```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "filtering-with-reactor-order-placed")
record FilteringWithReactorOrderPlaced(String customerId, double totalAmount) {}

// --- Append call ---
// Carries the "premium" tag for orders that qualify
// EventLogJavaBridge.append(eventStore.getEventLog(), orderId,
//     new FilteringWithReactorOrderPlaced(customerId, total),
//     new AppendOptionsBuilder().tag("premium").build());

// --- Projection: receives every OrderPlaced ---
@ReadModel
@FromEvent(eventType = FilteringWithReactorOrderPlaced.class)
record FilteringWithReactorOrderSummary(String customerId, double totalAmount) {}

// --- Reactor: receives only premium-tagged OrderPlaced ---
@Reactor
@FilterEventsByTag("premium")
class FilteringWithReactorPremiumOrderNotifier {
    void placed(FilteringWithReactorOrderPlaced event, EventContext context) {
    }
}
```
