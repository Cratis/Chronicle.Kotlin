```kotlin
import io.cratis.chronicle.IEventStore

class GetStartedReadModels(private val eventStore: IEventStore) {
    suspend fun books(): List<GetStartedBookDetails> =
        eventStore.readModels.getInstances(GetStartedBookDetails::class)

    suspend fun borrowedBooks(): List<GetStartedBorrowedBook> =
        eventStore.readModels.getInstances(GetStartedBorrowedBook::class)
}
```
