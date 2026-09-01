```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ReducersAsyncSignaturesOpened(String orderId) {
}

@EventType
record ReducersAsyncSignaturesItemAdded(String sku, double amount) {
}

@ReadModel
record ReducersAsyncSignaturesOrder(String orderId, double total, String currency) {
}

class ReducersAsyncSignaturesRates {
    // Stands in for whatever you actually call - an HTTP client, a cache, a database.
    String currencyFor(String sku) {
        return sku.startsWith("EU") ? "EUR" : "USD";
    }
}

@Reducer
class ReducersAsyncSignaturesOrderReducer {
    private final ReducersAsyncSignaturesRates rates = new ReducersAsyncSignaturesRates();

    // Every shape Kotlin has is available: (event), (event, current) and
    // (event, current, context).
    ReducersAsyncSignaturesOrder opened(ReducersAsyncSignaturesOpened event) {
        return new ReducersAsyncSignaturesOrder(event.orderId(), 0, "");
    }

    // Java has no `suspend`, so a handler that calls out to something blocks the thread the
    // observation runs on. Keep that work short, or hand it to your own executor and await the
    // result here - the observation only moves on once the handler returns.
    ReducersAsyncSignaturesOrder itemAdded(
            ReducersAsyncSignaturesItemAdded event,
            ReducersAsyncSignaturesOrder current,
            EventContext context) {
        ReducersAsyncSignaturesOrder order = current != null
            ? current
            : new ReducersAsyncSignaturesOrder(context.getEventSourceId(), 0, "");

        return new ReducersAsyncSignaturesOrder(
            order.orderId(),
            order.total() + event.amount(),
            rates.currencyFor(event.sku()));
    }
}
```
