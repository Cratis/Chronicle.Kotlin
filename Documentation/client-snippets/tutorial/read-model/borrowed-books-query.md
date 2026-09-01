```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TutorialBorrowedBook(val title: String = "", val memberName: String = "")

/**
 * Kotlin has no raw MongoCollection to query directly - reads go through the read model service
 * instead.
 */
class BorrowedBooks(private val store: IEventStore) {
    suspend fun all(): List<TutorialBorrowedBook> = store.readModels.getInstances(TutorialBorrowedBook::class)
}
```
