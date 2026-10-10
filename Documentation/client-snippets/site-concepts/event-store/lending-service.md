```kotlin
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import java.time.LocalDate

@EventType
data class BookBorrowed(val member: String, val dueDate: LocalDate)

class LendingService(private val eventLog: IEventLog) {
    suspend fun lend(bookId: String, member: String, dueDate: LocalDate) =
        eventLog.append(bookId, BookBorrowed(member, dueDate))
}
```
