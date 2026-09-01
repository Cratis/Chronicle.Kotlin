```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "side-effects-book-reserved")
data class SideEffectsBookReserved(val isbn: String)

@EventType(id = "side-effects-stock-check-requested")
data class SideEffectsStockCheckRequested(val isbn: String)

@EventType(id = "side-effects-stock-decreased")
data class StockDecreased(val isbn: String, val quantity: Int)

@Reactor
class WarehouseReactor {
    // Return the event directly - it is appended to the event log using the EventSourceId from
    // the incoming event.
    fun bookReserved(event: SideEffectsBookReserved, context: EventContext) =
        StockDecreased(event.isbn, 1)

    // A handler may suspend before returning its side-effect event.
    suspend fun stockCheckRequested(event: SideEffectsStockCheckRequested, context: EventContext): StockDecreased {
        val available = fetchCurrentStock(event.isbn)
        return StockDecreased(event.isbn, available)
    }

    private suspend fun fetchCurrentStock(isbn: String): Int = 0
}
```
