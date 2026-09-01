```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "side-effects-multiple-book-reserved")
data class MultipleSideEffectsBookReserved(val isbn: String)

@EventType(id = "side-effects-multiple-stock-decreased")
data class MultipleStockDecreased(val isbn: String, val quantity: Int)

@EventType(id = "side-effects-stock-low")
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
