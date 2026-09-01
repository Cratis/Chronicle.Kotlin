```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "side-effects-book-reserved")
record SideEffectsBookReserved(String isbn) {}

@EventType(id = "side-effects-stock-check-requested")
record SideEffectsStockCheckRequested(String isbn) {}

@EventType(id = "side-effects-stock-decreased")
record StockDecreased(String isbn, int quantity) {}

@Reactor
class WarehouseReactor {
    // Return the event directly - it is appended to the event log using the EventSourceId from
    // the incoming event.
    StockDecreased bookReserved(SideEffectsBookReserved event, EventContext context) {
        return new StockDecreased(event.isbn(), 1);
    }

    StockDecreased stockCheckRequested(SideEffectsStockCheckRequested event, EventContext context) {
        int available = fetchCurrentStock(event.isbn());
        return new StockDecreased(event.isbn(), available);
    }

    private int fetchCurrentStock(String isbn) {
        return 0;
    }
}
```
