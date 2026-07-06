```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import java.util.UUID

@EventType
data class EcBookCreated(val title: String, val author: String)

data class EcBookInventory(val id: String = "", val title: String = "", val author: String = "")

class EcBookService(private val store: IEventStore) {
    // Good — fire and forget: don't wait for the projection before returning
    suspend fun createBook(title: String, author: String): String {
        val bookId = UUID.randomUUID().toString()
        store.eventLog.append(bookId, EcBookCreated(title, author))
        return bookId
    }

    // Problematic — expecting immediate consistency
    suspend fun createBookAndReturn(title: String, author: String): EcBookInventory? {
        val bookId = UUID.randomUUID().toString()
        store.eventLog.append(bookId, EcBookCreated(title, author))

        // The projection may not have run yet — this can return null or a stale instance
        return store.readModels.getInstanceByKey(EcBookInventory::class, bookId)
    }
}
```
