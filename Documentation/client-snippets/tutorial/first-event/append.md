```kotlin
import io.cratis.chronicle.IEventStore
import java.util.UUID

class TutorialFirstEventAppend {
    suspend fun addBook(eventStore: IEventStore): String {
        val bookId = UUID.randomUUID().toString()
        eventStore.eventLog.append(bookId, BookAdded("The Pragmatic Programmer", "978-0135957059"))
        return bookId
    }
}
```
