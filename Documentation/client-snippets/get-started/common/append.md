```kotlin
import io.cratis.chronicle.IEventStore
import java.util.UUID

class GetStartedBookService(private val eventStore: IEventStore) {
    suspend fun addBook(): String {
        val eventLog = eventStore.eventLog

        val bookId = UUID.randomUUID().toString()
        eventLog.append(bookId, GetStartedBookAdded("The Pragmatic Programmer", "978-0135957059"))

        return bookId
    }
}
```
