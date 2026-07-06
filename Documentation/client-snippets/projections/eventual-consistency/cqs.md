```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class EcCqsBookCreated(val title: String)

data class EcCqsBook(val id: String = "", val title: String = "")

// Commands — fire and forget, never return projected state
class EcCqsBookCommandHandler(private val store: IEventStore) {
    suspend fun create(bookId: String, title: String) {
        store.eventLog.append(bookId, EcCqsBookCreated(title))
    }
}

// Queries — always read from projections
class EcCqsBookQueryHandler(private val store: IEventStore) {
    suspend fun getBook(bookId: String): EcCqsBook? =
        store.readModels.getInstanceByKey(EcCqsBook::class, bookId)
}
```
