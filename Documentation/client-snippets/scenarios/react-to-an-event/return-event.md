```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType
data class ScenariosReactBookReserved(val isbn: String)

@EventType
data class ScenariosReactStockDecreased(val isbn: String, val quantity: Int)

// Returning an event from a handler appends it as a side effect - against the event source that
// triggered the reactor - with no event log dependency in sight.
@Reactor
class ScenariosReactStockKeeping {
    fun bookReserved(event: ScenariosReactBookReserved, context: EventContext): ScenariosReactStockDecreased =
        ScenariosReactStockDecreased(event.isbn, 1)
}
```
