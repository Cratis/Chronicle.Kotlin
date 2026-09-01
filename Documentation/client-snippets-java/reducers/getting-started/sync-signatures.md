```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ReducersSignaturesOpened(String orderId) {}

@EventType
record ReducersSignaturesItemAdded(double amount) {}

@EventType
record ReducersSignaturesClosed(String reason) {}

@ReadModel
record ReducersSignaturesOrder(String orderId, double total, String closedAt) {
    ReducersSignaturesOrder() {
        this("", 0.0, "");
    }
}

@Reducer
class ReducersSignaturesOrderReducer {
    // (event) - the state so far is not needed
    ReducersSignaturesOrder opened(ReducersSignaturesOpened event) {
        return new ReducersSignaturesOrder(event.orderId(), 0.0, "");
    }

    // (event, current) - current is null until the first event for an event source
    ReducersSignaturesOrder itemAdded(ReducersSignaturesItemAdded event, ReducersSignaturesOrder current) {
        if (current == null) return null;
        return new ReducersSignaturesOrder(
            current.orderId(),
            current.total() + event.amount(),
            current.closedAt());
    }

    // (event, current, context) - context carries the event's metadata
    ReducersSignaturesOrder closed(
            ReducersSignaturesClosed event,
            ReducersSignaturesOrder current,
            EventContext context) {
        if (current == null) return null;
        return new ReducersSignaturesOrder(
            current.orderId(),
            current.total(),
            context.getOccurred().toString());
    }
}
```
