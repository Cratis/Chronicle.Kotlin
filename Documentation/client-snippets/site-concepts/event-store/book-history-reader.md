```kotlin
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.IEventLog

class BookHistoryReader(private val eventLog: IEventLog) {
    suspend fun getBorrowings(bookId: String): List<AppendedEvent> =
        eventLog.getForEventSourceIdAndEventTypes(bookId, listOf(BookBorrowed::class))
}
```
