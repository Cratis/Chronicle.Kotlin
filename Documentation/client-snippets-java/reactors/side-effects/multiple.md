```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

import java.util.List;

@EventType(id = "side-effects-multiple-book-reserved")
record MultipleSideEffectsBookReserved(String isbn) {}

@EventType(id = "side-effects-multiple-stock-decreased")
record MultipleStockDecreased(String isbn, int quantity) {}

@EventType(id = "side-effects-stock-low")
record StockLow(String isbn) {}

@Reactor
class InventoryReactor {
    // Return a List to append several events in one handler call.
    List<Object> bookReserved(MultipleSideEffectsBookReserved event, EventContext context) {
        return List.of(
            new MultipleStockDecreased(event.isbn(), 1),
            new StockLow(event.isbn()));
    }
}
```
