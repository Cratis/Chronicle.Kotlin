```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType
data class MultipleSideEffectsBookReserved(val isbn: String)

@EventType
data class MultipleStockDecreased(val isbn: String, val quantity: Int)

@EventType
data class StockLow(val isbn: String)

@Reactor
class InventoryReactor {
    // Return a List to append several events in one handler call.
    fun bookReserved(event: MultipleSideEffectsBookReserved, context: EventContext): List<Any> = listOf(
        MultipleStockDecreased(event.isbn, 1),
        StockLow(event.isbn)
    )
}
```
