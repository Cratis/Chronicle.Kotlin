```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType
record ScenariosReactBookReserved(String isbn) {}

@EventType
record ScenariosReactStockDecreased(String isbn, int quantity) {}

// Returning an event from a handler appends it as a side effect - against the event source that
// triggered the reactor - with no event log dependency in sight.
@Reactor
class ScenariosReactStockKeeping {
    ScenariosReactStockDecreased bookReserved(ScenariosReactBookReserved event, EventContext context) {
        return new ScenariosReactStockDecreased(event.isbn(), 1);
    }
}
```
