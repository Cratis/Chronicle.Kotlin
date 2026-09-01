```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TutorialBook(val title: String = "", val onLoan: Boolean = false)

/**
 * Kotlin has no raw MongoCollection to query directly - reads go through the read model service
 * instead, which returns already-deserialized instances to filter in memory.
 */
class Books(private val store: IEventStore) {
    suspend fun onLoan(): List<TutorialBook> =
        store.readModels.getInstances(TutorialBook::class).filter { it.onLoan }
}
```
