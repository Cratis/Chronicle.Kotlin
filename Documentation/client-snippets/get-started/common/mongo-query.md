```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class GetStartedBook(val title: String = "", val onLoan: Boolean = false)

/**
 * Kotlin has no raw MongoCollection to query directly - reads go through the read model service
 * instead, which returns already-deserialized instances to filter in memory.
 */
class GetStartedBooks(private val store: IEventStore) {
    suspend fun onLoan(): List<GetStartedBook> =
        store.readModels.getInstances(GetStartedBook::class).filter { it.onLoan }
}
```
